package com.ims.finance.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.AcademicCatalogService.FeeCategoryView;
import com.ims.common.money.Money;
import com.ims.common.tenancy.TenantContext;
import com.ims.finance.domain.Invoice;
import com.ims.finance.domain.Payment;
import com.ims.finance.domain.PaymentAllocation;
import com.ims.finance.domain.StudentFeeAccount;
import com.ims.finance.infrastructure.InvoiceJpaRepository;
import com.ims.finance.infrastructure.PaymentAllocationJpaRepository;
import com.ims.finance.infrastructure.PaymentJpaRepository;
import com.ims.finance.infrastructure.StudentFeeAccountJpaRepository;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FinanceReportService {

  private final InvoiceJpaRepository invoiceJpaRepository;
  private final PaymentJpaRepository paymentJpaRepository;
  private final PaymentAllocationJpaRepository paymentAllocationJpaRepository;
  private final StudentFeeAccountJpaRepository studentFeeAccountJpaRepository;
  private final AcademicCatalogService academicCatalogService;
  private final TenantFilterEnabler tenantFilterEnabler;

  public FinanceReportService(
      InvoiceJpaRepository invoiceJpaRepository,
      PaymentJpaRepository paymentJpaRepository,
      PaymentAllocationJpaRepository paymentAllocationJpaRepository,
      StudentFeeAccountJpaRepository studentFeeAccountJpaRepository,
      AcademicCatalogService academicCatalogService,
      TenantFilterEnabler tenantFilterEnabler) {
    this.invoiceJpaRepository = invoiceJpaRepository;
    this.paymentJpaRepository = paymentJpaRepository;
    this.paymentAllocationJpaRepository = paymentAllocationJpaRepository;
    this.studentFeeAccountJpaRepository = studentFeeAccountJpaRepository;
    this.academicCatalogService = academicCatalogService;
    this.tenantFilterEnabler = tenantFilterEnabler;
  }

  public record AgingBucket(String bucket, BigDecimal amount, long invoiceCount) {}

  public record AgingInvoiceRow(
      Long invoiceId,
      String invoiceNo,
      Long feeAccountId,
      Long studentId,
      LocalDate dueDate,
      long daysOverdue,
      String bucket,
      BigDecimal remaining,
      String status) {}

  public record AgingReport(
      LocalDate asOf, List<AgingBucket> buckets, List<AgingInvoiceRow> invoices) {}

  public record NamedAmount(String key, String label, BigDecimal amount, long count) {}

  public record DailyCollectionRow(LocalDate date, BigDecimal amount, long paymentCount) {}

  public record OutstandingRow(
      Long feeAccountId,
      Long studentId,
      Long enrollmentId,
      String currency,
      BigDecimal outstandingAmount) {}

  @Transactional(readOnly = true)
  public AgingReport aging(LocalDate asOf) {
    long instituteId = requireTenant();
    LocalDate day = asOf == null ? LocalDate.now(ZoneOffset.UTC) : asOf;
    List<Invoice> open =
        invoiceJpaRepository.findByInstituteIdAndStatusInOrderByDueDateAscIdAsc(
            instituteId, List.of(Invoice.STATUS_DUE, Invoice.STATUS_PARTIAL));
    Map<Long, StudentFeeAccount> accounts = new HashMap<>();
    Map<String, BigDecimal> bucketAmounts = new LinkedHashMap<>();
    Map<String, Long> bucketCounts = new LinkedHashMap<>();
    for (String key : List.of("CURRENT", "1-30", "31-60", "61-90", "90+")) {
      bucketAmounts.put(key, BigDecimal.ZERO.setScale(2));
      bucketCounts.put(key, 0L);
    }
    List<AgingInvoiceRow> rows = new ArrayList<>();
    for (Invoice invoice : open) {
      BigDecimal remaining = Money.normalize(invoice.remaining());
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      long days = ChronoUnit.DAYS.between(invoice.getDueDate(), day);
      String bucket = bucketFor(days);
      bucketAmounts.put(bucket, Money.normalize(bucketAmounts.get(bucket).add(remaining)));
      bucketCounts.put(bucket, bucketCounts.get(bucket) + 1);
      StudentFeeAccount account =
          accounts.computeIfAbsent(
              invoice.getFeeAccountId(),
              id ->
                  studentFeeAccountJpaRepository
                      .findByIdAndInstituteId(id, instituteId)
                      .orElse(null));
      rows.add(
          new AgingInvoiceRow(
              invoice.getId(),
              invoice.getInvoiceNo(),
              invoice.getFeeAccountId(),
              account == null ? null : account.getStudentId(),
              invoice.getDueDate(),
              Math.max(days, 0),
              bucket,
              remaining,
              invoice.getStatus()));
    }
    List<AgingBucket> buckets =
        bucketAmounts.entrySet().stream()
            .map(e -> new AgingBucket(e.getKey(), e.getValue(), bucketCounts.get(e.getKey())))
            .toList();
    return new AgingReport(day, buckets, rows);
  }

  @Transactional(readOnly = true)
  public List<OutstandingRow> outstandingByStudent() {
    long instituteId = requireTenant();
    return studentFeeAccountJpaRepository
        .findByInstituteIdAndStatusAndOutstandingAmountGreaterThan(
            instituteId, StudentFeeAccount.STATUS_OPEN, BigDecimal.ZERO)
        .stream()
        .sorted(Comparator.comparing(StudentFeeAccount::getOutstandingAmount).reversed())
        .map(
            a ->
                new OutstandingRow(
                    a.getId(),
                    a.getStudentId(),
                    a.getEnrollmentId(),
                    a.getCurrency(),
                    a.getOutstandingAmount()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DailyCollectionRow> dailyCollection(LocalDate from, LocalDate to) {
    List<Payment> payments = successPayments(from, to);
    Map<LocalDate, List<Payment>> byDay =
        payments.stream()
            .collect(
                Collectors.groupingBy(
                    p -> LocalDate.ofInstant(p.getPaidAt(), ZoneOffset.UTC),
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<DailyCollectionRow> rows = new ArrayList<>();
    byDay.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e -> {
              BigDecimal sum =
                  e.getValue().stream()
                      .map(Payment::getAmount)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              rows.add(new DailyCollectionRow(e.getKey(), Money.normalize(sum), e.getValue().size()));
            });
    return rows;
  }

  @Transactional(readOnly = true)
  public List<NamedAmount> collectionByMethod(LocalDate from, LocalDate to) {
    List<Payment> payments = successPayments(from, to);
    Map<String, List<Payment>> byMethod =
        payments.stream().collect(Collectors.groupingBy(Payment::getMethod, LinkedHashMap::new, Collectors.toList()));
    return byMethod.entrySet().stream()
        .map(
            e -> {
              BigDecimal sum =
                  e.getValue().stream()
                      .map(Payment::getAmount)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              return new NamedAmount(e.getKey(), e.getKey(), Money.normalize(sum), e.getValue().size());
            })
        .sorted(Comparator.comparing(NamedAmount::amount).reversed())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<NamedAmount> collectionByCategory(LocalDate from, LocalDate to) {
    long instituteId = requireTenant();
    List<Payment> payments = successPayments(from, to);
    if (payments.isEmpty()) {
      return List.of();
    }
    List<Long> paymentIds = payments.stream().map(Payment::getId).toList();
    List<PaymentAllocation> allocations =
        paymentAllocationJpaRepository.findByInstituteIdAndPaymentIdIn(instituteId, paymentIds);
    Map<Long, String> categoryNames =
        academicCatalogService.listCategories().stream()
            .collect(Collectors.toMap(FeeCategoryView::id, FeeCategoryView::name, (a, b) -> a));
    Map<Long, BigDecimal> byCategory = new HashMap<>();
    Map<Long, Long> counts = new HashMap<>();
    for (PaymentAllocation allocation : allocations) {
      Invoice invoice =
          invoiceJpaRepository
              .findByIdAndInstituteId(allocation.getInvoiceId(), instituteId)
              .orElse(null);
      if (invoice == null) {
        continue;
      }
      Long categoryId = invoice.getFeeCategoryId();
      byCategory.merge(categoryId, Money.normalize(allocation.getAmount()), BigDecimal::add);
      counts.merge(categoryId, 1L, Long::sum);
    }
    return byCategory.entrySet().stream()
        .map(
            e ->
                new NamedAmount(
                    String.valueOf(e.getKey()),
                    categoryNames.getOrDefault(e.getKey(), "Category #" + e.getKey()),
                    Money.normalize(e.getValue()),
                    counts.getOrDefault(e.getKey(), 0L)))
        .sorted(Comparator.comparing(NamedAmount::amount).reversed())
        .toList();
  }

  private List<Payment> successPayments(LocalDate from, LocalDate to) {
    long instituteId = requireTenant();
    LocalDateRange range = normalizeRange(from, to);
    Instant fromInstant = range.from().atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant toExclusive = range.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return paymentJpaRepository.findByInstituteIdAndStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
        instituteId, Payment.STATUS_SUCCESS, fromInstant, toExclusive);
  }

  private record LocalDateRange(LocalDate from, LocalDate to) {}

  private LocalDateRange normalizeRange(LocalDate from, LocalDate to) {
    LocalDate end = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
    LocalDate start = from == null ? end.minusDays(30) : from;
    if (start.isAfter(end)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date must be on or before to date");
    }
    if (ChronoUnit.DAYS.between(start, end) > 366) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date range cannot exceed 366 days");
    }
    return new LocalDateRange(start, end);
  }

  private static String bucketFor(long daysPastDue) {
    if (daysPastDue <= 0) {
      return "CURRENT";
    }
    if (daysPastDue <= 30) {
      return "1-30";
    }
    if (daysPastDue <= 60) {
      return "31-60";
    }
    if (daysPastDue <= 90) {
      return "61-90";
    }
    return "90+";
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }
}

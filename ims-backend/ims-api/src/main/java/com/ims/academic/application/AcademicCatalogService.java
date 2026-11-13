package com.ims.academic.application;

import com.ims.academic.domain.AcademicYear;
import com.ims.academic.domain.Batch;
import com.ims.academic.domain.Course;
import com.ims.academic.domain.CourseFeeInstallment;
import com.ims.academic.domain.CourseFeePlan;
import com.ims.academic.domain.FeeCategory;
import com.ims.academic.infrastructure.AcademicYearJpaRepository;
import com.ims.academic.infrastructure.BatchJpaRepository;
import com.ims.academic.infrastructure.CourseFeeInstallmentJpaRepository;
import com.ims.academic.infrastructure.CourseFeePlanJpaRepository;
import com.ims.academic.infrastructure.CourseJpaRepository;
import com.ims.academic.infrastructure.FeeCategoryJpaRepository;
import com.ims.common.money.Money;
import com.ims.common.paging.PageResponse;
import com.ims.common.tenancy.TenantContext;
import com.ims.platform.codes.application.CodeGeneratorService;
import com.ims.platform.codes.domain.CodeEntityType;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademicCatalogService {

  private static final Logger log = LoggerFactory.getLogger(AcademicCatalogService.class);

  private final AcademicYearJpaRepository academicYearJpaRepository;
  private final FeeCategoryJpaRepository feeCategoryJpaRepository;
  private final CourseJpaRepository courseJpaRepository;
  private final CourseFeePlanJpaRepository courseFeePlanJpaRepository;
  private final CourseFeeInstallmentJpaRepository courseFeeInstallmentJpaRepository;
  private final BatchJpaRepository batchJpaRepository;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final CodeGeneratorService codeGeneratorService;

  public AcademicCatalogService(
      AcademicYearJpaRepository academicYearJpaRepository,
      FeeCategoryJpaRepository feeCategoryJpaRepository,
      CourseJpaRepository courseJpaRepository,
      CourseFeePlanJpaRepository courseFeePlanJpaRepository,
      CourseFeeInstallmentJpaRepository courseFeeInstallmentJpaRepository,
      BatchJpaRepository batchJpaRepository,
      TenantFilterEnabler tenantFilterEnabler,
      CodeGeneratorService codeGeneratorService) {
    this.academicYearJpaRepository = academicYearJpaRepository;
    this.feeCategoryJpaRepository = feeCategoryJpaRepository;
    this.courseJpaRepository = courseJpaRepository;
    this.courseFeePlanJpaRepository = courseFeePlanJpaRepository;
    this.courseFeeInstallmentJpaRepository = courseFeeInstallmentJpaRepository;
    this.batchJpaRepository = batchJpaRepository;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.codeGeneratorService = codeGeneratorService;
  }

  public record AcademicYearView(
      Long id, String code, String name, LocalDate startDate, LocalDate endDate, String status) {}

  public record FeeCategoryView(Long id, String code, String name) {}

  public record CourseView(Long id, String code, String name, String description, String status) {}

  public record InstallmentView(
      Long id, int seq, Long feeCategoryId, String label, BigDecimal amount, int dueOffsetDays) {}

  public record FeePlanView(
      Long id,
      Long courseId,
      String code,
      String name,
      String currency,
      BigDecimal totalAmount,
      String status,
      List<InstallmentView> installments) {}

  public record BatchView(
      Long id,
      Long courseId,
      Long academicYearId,
      String code,
      String name,
      int capacity,
      String status,
      LocalDate startDate,
      LocalDate endDate) {}

  public record InstallmentInput(
      int seq, Long feeCategoryId, String label, BigDecimal amount, int dueOffsetDays) {}

  @Transactional(readOnly = true)
  public List<AcademicYearView> listYears() {
    long instituteId = requireTenant();
    return academicYearJpaRepository.findByInstituteIdOrderByStartDateDesc(instituteId).stream()
        .map(this::toYear)
        .toList();
  }

  @Transactional
  public AcademicYearView createYear(
      String code, String name, LocalDate startDate, LocalDate endDate) {
    long instituteId = requireTenant();
    if (endDate.isBefore(startDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on/after startDate");
    }
    String normalized = codeGeneratorService.resolve(CodeEntityType.ACADEMIC_YEAR, code);
    if (academicYearJpaRepository.existsByInstituteIdAndCodeIgnoreCase(instituteId, normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Academic year code already exists");
    }
    AcademicYear saved =
        academicYearJpaRepository.save(
            AcademicYear.create(instituteId, normalized, name, startDate, endDate));
    log.info("Academic year created id={} code={} instituteId={}", saved.getId(), saved.getCode(), instituteId);
    return toYear(saved);
  }

  @Transactional(readOnly = true)
  public List<FeeCategoryView> listCategories() {
    long instituteId = requireTenant();
    return feeCategoryJpaRepository.findByInstituteIdOrderByCodeAsc(instituteId).stream()
        .map(this::toCategory)
        .toList();
  }

  @Transactional
  public FeeCategoryView createCategory(String code, String name) {
    long instituteId = requireTenant();
    String normalized = codeGeneratorService.resolve(CodeEntityType.FEE_CATEGORY, code);
    if (feeCategoryJpaRepository.existsByInstituteIdAndCodeIgnoreCase(instituteId, normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Fee category code already exists");
    }
    FeeCategory saved =
        feeCategoryJpaRepository.save(FeeCategory.create(instituteId, normalized, name));
    log.info("Fee category created id={} code={} instituteId={}", saved.getId(), saved.getCode(), instituteId);
    return toCategory(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseView> listCourses(String q, int page, int size) {
    long instituteId = requireTenant();
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    Page<Course> result =
        courseJpaRepository.search(instituteId, q == null ? "" : q.trim(), PageRequest.of(safePage, safeSize));
    return PageResponse.of(
        result.getContent().stream().map(this::toCourse).toList(),
        safePage,
        safeSize,
        result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public CourseView getCourse(Long id) {
    return toCourse(requireCourse(id));
  }

  @Transactional
  public CourseView createCourse(String code, String name, String description) {
    long instituteId = requireTenant();
    String normalized = codeGeneratorService.resolve(CodeEntityType.COURSE, code);
    if (courseJpaRepository.existsByInstituteIdAndCodeIgnoreCase(instituteId, normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists");
    }
    Course saved =
        courseJpaRepository.save(Course.create(instituteId, normalized, name, description));
    log.info("Course created id={} code={} instituteId={}", saved.getId(), saved.getCode(), instituteId);
    return toCourse(saved);
  }

  @Transactional
  public CourseView updateCourse(Long id, String name, String description) {
    Course course = requireCourse(id);
    course.update(name, description);
    return toCourse(courseJpaRepository.save(course));
  }

  @Transactional
  public CourseView deactivateCourse(Long id) {
    Course course = requireCourse(id);
    course.deactivate();
    Course saved = courseJpaRepository.save(course);
    log.info("Course deactivated id={} code={}", saved.getId(), saved.getCode());
    return toCourse(saved);
  }

  @Transactional(readOnly = true)
  public List<FeePlanView> listFeePlans(Long courseId) {
    long instituteId = requireTenant();
    requireCourse(courseId);
    return courseFeePlanJpaRepository.findByInstituteIdAndCourseIdOrderByCodeAsc(instituteId, courseId)
        .stream()
        .map(this::toPlan)
        .toList();
  }

  @Transactional(readOnly = true)
  public FeePlanView getFeePlan(Long planId) {
    return toPlan(requirePlan(planId));
  }

  @Transactional
  public FeePlanView createFeePlan(
      Long courseId,
      String code,
      String name,
      String currency,
      BigDecimal totalAmount,
      List<InstallmentInput> installments) {
    long instituteId = requireTenant();
    requireCourse(courseId);
    String normalized = codeGeneratorService.resolve(CodeEntityType.FEE_PLAN, code);
    if (courseFeePlanJpaRepository.existsByInstituteIdAndCodeIgnoreCase(instituteId, normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Fee plan code already exists");
    }
    BigDecimal total = Money.normalize(totalAmount);
    validateInstallmentSum(total, installments);
    CourseFeePlan plan =
        courseFeePlanJpaRepository.save(
            CourseFeePlan.create(instituteId, courseId, normalized, name, currency, total));
    saveInstallments(instituteId, plan.getId(), installments);
    log.info("Fee plan created id={} code={} courseId={}", plan.getId(), plan.getCode(), courseId);
    return toPlan(plan);
  }

  @Transactional
  public FeePlanView replaceFeePlanInstallments(
      Long planId, String name, BigDecimal totalAmount, List<InstallmentInput> installments) {
    long instituteId = requireTenant();
    CourseFeePlan plan = requirePlan(planId);
    BigDecimal total = Money.normalize(totalAmount);
    validateInstallmentSum(total, installments);
    plan.update(name, total);
    courseFeePlanJpaRepository.save(plan);
    courseFeeInstallmentJpaRepository.deleteByFeePlanIdAndInstituteId(planId, instituteId);
    saveInstallments(instituteId, planId, installments);
    return toPlan(plan);
  }

  @Transactional(readOnly = true)
  public List<BatchView> listBatches(Long courseId) {
    long instituteId = requireTenant();
    List<Batch> batches =
        courseId == null
            ? batchJpaRepository.findByInstituteIdOrderByCodeAsc(instituteId)
            : batchJpaRepository.findByInstituteIdAndCourseIdOrderByCodeAsc(instituteId, courseId);
    return batches.stream().map(this::toBatch).toList();
  }

  @Transactional(readOnly = true)
  public BatchView getBatch(Long id) {
    return toBatch(requireBatch(id));
  }

  @Transactional
  public BatchView createBatch(
      Long courseId,
      Long academicYearId,
      String code,
      String name,
      int capacity,
      LocalDate startDate,
      LocalDate endDate) {
    long instituteId = requireTenant();
    requireCourse(courseId);
    requireYear(academicYearId);
    if (capacity < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "capacity must be >= 1");
    }
    String normalized = codeGeneratorService.resolve(CodeEntityType.BATCH, code);
    if (batchJpaRepository.existsByInstituteIdAndCodeIgnoreCase(instituteId, normalized)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch code already exists");
    }
    Batch saved =
        batchJpaRepository.save(
            Batch.create(
                instituteId, courseId, academicYearId, normalized, name, capacity, startDate, endDate));
    log.info("Batch created id={} code={} instituteId={}", saved.getId(), saved.getCode(), instituteId);
    return toBatch(saved);
  }

  @Transactional
  public BatchView updateBatch(
      Long id, String name, int capacity, String status, LocalDate startDate, LocalDate endDate) {
    if (capacity < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "capacity must be >= 1");
    }
    Batch batch = requireBatch(id);
    batch.update(name, capacity, status, startDate, endDate);
    return toBatch(batchJpaRepository.save(batch));
  }

  private void saveInstallments(long instituteId, Long planId, List<InstallmentInput> installments) {
    List<InstallmentInput> sorted =
        installments.stream().sorted(Comparator.comparingInt(InstallmentInput::seq)).toList();
    List<CourseFeeInstallment> rows = new ArrayList<>();
    for (InstallmentInput input : sorted) {
      requireCategory(input.feeCategoryId());
      rows.add(
          CourseFeeInstallment.create(
              instituteId,
              planId,
              input.seq(),
              input.feeCategoryId(),
              input.label(),
              Money.normalize(input.amount()),
              Math.max(input.dueOffsetDays(), 0)));
    }
    courseFeeInstallmentJpaRepository.saveAll(rows);
  }

  private void validateInstallmentSum(BigDecimal total, List<InstallmentInput> installments) {
    if (installments == null || installments.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one installment is required");
    }
    BigDecimal sum =
        installments.stream()
            .map(i -> Money.normalize(i.amount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(total) != 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Installment amounts must sum to fee plan total");
    }
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private Course requireCourse(Long id) {
    long instituteId = requireTenant();
    return courseJpaRepository
        .findByIdAndInstituteIdAndDeletedAtIsNull(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
  }

  private CourseFeePlan requirePlan(Long id) {
    long instituteId = requireTenant();
    return courseFeePlanJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee plan not found"));
  }

  private FeeCategory requireCategory(Long id) {
    long instituteId = requireTenant();
    return feeCategoryJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));
  }

  private AcademicYear requireYear(Long id) {
    long instituteId = requireTenant();
    return academicYearJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));
  }

  private Batch requireBatch(Long id) {
    long instituteId = requireTenant();
    return batchJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));
  }

  private AcademicYearView toYear(AcademicYear y) {
    return new AcademicYearView(
        y.getId(), y.getCode(), y.getName(), y.getStartDate(), y.getEndDate(), y.getStatus());
  }

  private FeeCategoryView toCategory(FeeCategory c) {
    return new FeeCategoryView(c.getId(), c.getCode(), c.getName());
  }

  private CourseView toCourse(Course c) {
    return new CourseView(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.getStatus());
  }

  private FeePlanView toPlan(CourseFeePlan plan) {
    long instituteId = plan.getInstituteId();
    List<InstallmentView> installments =
        courseFeeInstallmentJpaRepository
            .findByFeePlanIdAndInstituteIdOrderBySeqAsc(plan.getId(), instituteId)
            .stream()
            .map(
                i ->
                    new InstallmentView(
                        i.getId(),
                        i.getSeq(),
                        i.getFeeCategoryId(),
                        i.getLabel(),
                        i.getAmount(),
                        i.getDueOffsetDays()))
            .toList();
    return new FeePlanView(
        plan.getId(),
        plan.getCourseId(),
        plan.getCode(),
        plan.getName(),
        plan.getCurrency(),
        plan.getTotalAmount(),
        plan.getStatus(),
        installments);
  }

  private BatchView toBatch(Batch b) {
    return new BatchView(
        b.getId(),
        b.getCourseId(),
        b.getAcademicYearId(),
        b.getCode(),
        b.getName(),
        b.getCapacity(),
        b.getStatus(),
        b.getStartDate(),
        b.getEndDate());
  }
}

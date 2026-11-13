package com.ims.admissions.application;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.admissions.api.AdmissionResponse;
import com.ims.admissions.api.EnquiryResponse;
import com.ims.admissions.application.AdmissionService.CreateAdmissionCommand;
import com.ims.admissions.domain.Enquiry;
import com.ims.admissions.infrastructure.EnquiryJpaRepository;
import com.ims.common.paging.PageResponse;
import com.ims.common.tenancy.TenantContext;
import com.ims.platform.audit.application.AuditService;
import com.ims.platform.tenancy.infrastructure.TenantFilterEnabler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnquiryService {

  private static final Logger log = LoggerFactory.getLogger(EnquiryService.class);

  private final EnquiryJpaRepository enquiryJpaRepository;
  private final AdmissionService admissionService;
  private final AcademicCatalogService academicCatalogService;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final AuditService auditService;

  public EnquiryService(
      EnquiryJpaRepository enquiryJpaRepository,
      AdmissionService admissionService,
      AcademicCatalogService academicCatalogService,
      TenantFilterEnabler tenantFilterEnabler,
      AuditService auditService) {
    this.enquiryJpaRepository = enquiryJpaRepository;
    this.admissionService = admissionService;
    this.academicCatalogService = academicCatalogService;
    this.tenantFilterEnabler = tenantFilterEnabler;
    this.auditService = auditService;
  }

  public record CreateEnquiryCommand(
      @NotBlank @Size(max = 128) String name,
      @Size(max = 32) String phone,
      @Size(max = 255) String email,
      Long interestedCourseId,
      @Size(max = 1024) String notes) {}

  public record CloseEnquiryCommand(@Size(max = 1024) String notes) {}

  public record ConvertEnquiryCommand(
      @Size(max = 32) String applicationNo, Long courseId) {}

  @Transactional
  public EnquiryResponse create(CreateEnquiryCommand command) {
    long instituteId = requireTenant();
    if (command.interestedCourseId() != null) {
      academicCatalogService.getCourse(command.interestedCourseId());
    }
    Enquiry saved =
        enquiryJpaRepository.save(
            Enquiry.create(
                instituteId,
                command.name(),
                command.phone(),
                command.email(),
                command.interestedCourseId(),
                command.notes()));
    log.info("Enquiry created id={} instituteId={}", saved.getId(), instituteId);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<EnquiryResponse> list(String q, String status, int page, int size) {
    long instituteId = requireTenant();
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    String statusFilter =
        status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())
            ? null
            : status.trim().toUpperCase();
    Page<Enquiry> result =
        enquiryJpaRepository.search(
            instituteId,
            q == null ? "" : q.trim(),
            statusFilter,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    List<EnquiryResponse> content = result.getContent().stream().map(this::toResponse).toList();
    return PageResponse.of(content, safePage, safeSize, result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public EnquiryResponse get(Long id) {
    return toResponse(requireEnquiry(id));
  }

  @Transactional
  public EnquiryResponse close(Long id, CloseEnquiryCommand command) {
    Enquiry enquiry = requireEnquiry(id);
    enquiry.close(command == null ? null : command.notes());
    enquiryJpaRepository.save(enquiry);
    auditService.record(
        enquiry.getInstituteId(),
        "admissions",
        "Enquiry",
        enquiry.getId(),
        "ENQUIRY_CLOSED",
        null,
        Map.of("status", enquiry.getStatus()),
        command == null ? null : command.notes());
    return toResponse(enquiry);
  }

  @Transactional
  public AdmissionResponse convert(Long id, ConvertEnquiryCommand command) {
    Enquiry enquiry = requireEnquiry(id);
    if (!Enquiry.STATUS_OPEN.equals(enquiry.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only OPEN enquiries can be converted");
    }
    Long courseId =
        command != null && command.courseId() != null
            ? command.courseId()
            : enquiry.getInterestedCourseId();
    if (courseId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Course is required to convert this enquiry");
    }
    String applicationNo = command == null ? null : command.applicationNo();
    AdmissionResponse application =
        admissionService.create(
            new CreateAdmissionCommand(
                applicationNo,
                courseId,
                enquiry.getName(),
                enquiry.getPhone(),
                enquiry.getEmail(),
                enquiry.getId()));
    log.info(
        "Enquiry converted id={} applicationId={} instituteId={}",
        enquiry.getId(),
        application.id(),
        enquiry.getInstituteId());
    return application;
  }

  private Enquiry requireEnquiry(Long id) {
    long instituteId = requireTenant();
    return enquiryJpaRepository
        .findByIdAndInstituteId(id, instituteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enquiry not found"));
  }

  private long requireTenant() {
    long instituteId = TenantContext.requireInstituteId();
    tenantFilterEnabler.enableForCurrentTenant();
    return instituteId;
  }

  private EnquiryResponse toResponse(Enquiry enquiry) {
    return new EnquiryResponse(
        enquiry.getId(),
        enquiry.getName(),
        enquiry.getPhone(),
        enquiry.getEmail(),
        enquiry.getInterestedCourseId(),
        enquiry.getStatus(),
        enquiry.getNotes(),
        enquiry.getConvertedApplicationId(),
        enquiry.getCreatedAt());
  }
}

package com.ims.academic.api;

import com.ims.academic.application.AcademicCatalogService;
import com.ims.academic.application.AcademicCatalogService.AcademicYearView;
import com.ims.academic.application.AcademicCatalogService.BatchView;
import com.ims.academic.application.AcademicCatalogService.CourseView;
import com.ims.academic.application.AcademicCatalogService.FeeCategoryView;
import com.ims.academic.application.AcademicCatalogService.FeePlanView;
import com.ims.academic.application.AcademicCatalogService.InstallmentInput;
import com.ims.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Academic", description = "Courses, fee plans, batches, academic years")
@SecurityRequirement(name = "bearer-jwt")
public class AcademicCatalogController {

  private final AcademicCatalogService academicCatalogService;

  public AcademicCatalogController(AcademicCatalogService academicCatalogService) {
    this.academicCatalogService = academicCatalogService;
  }

  public record CreateYearRequest(
      @Size(max = 32) String code,
      @NotBlank @Size(max = 128) String name,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {}

  public record CreateCategoryRequest(
      @Size(max = 32) String code, @NotBlank @Size(max = 128) String name) {}

  public record CreateCourseRequest(
      @Size(max = 32) String code,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 512) String description) {}

  public record UpdateCourseRequest(
      @NotBlank @Size(max = 255) String name, @Size(max = 512) String description) {}

  public record InstallmentRequest(
      @Min(1) int seq,
      @NotNull Long feeCategoryId,
      @NotBlank @Size(max = 128) String label,
      @NotNull @DecimalMin("0.00") BigDecimal amount,
      @Min(0) int dueOffsetDays) {}

  public record CreateFeePlanRequest(
      @Size(max = 32) String code,
      @NotBlank @Size(max = 128) String name,
      @Size(max = 3) String currency,
      @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
      @NotEmpty List<@Valid InstallmentRequest> installments) {}

  public record UpdateFeePlanRequest(
      @NotBlank @Size(max = 128) String name,
      @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
      @NotEmpty List<@Valid InstallmentRequest> installments) {}

  public record CreateBatchRequest(
      @NotNull Long courseId,
      @NotNull Long academicYearId,
      @Size(max = 32) String code,
      @NotBlank @Size(max = 128) String name,
      @Min(1) int capacity,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {}

  public record UpdateBatchRequest(
      @NotBlank @Size(max = 128) String name,
      @Min(1) int capacity,
      @Size(max = 32) String status,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {}

  @GetMapping("/academic-years")
  @PreAuthorize("hasAuthority('course:read')")
  @Operation(summary = "List academic years")
  public List<AcademicYearView> listYears() {
    return academicCatalogService.listYears();
  }

  @PostMapping("/academic-years")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('course:write')")
  public AcademicYearView createYear(@Valid @RequestBody CreateYearRequest request) {
    return academicCatalogService.createYear(
        request.code(), request.name(), request.startDate(), request.endDate());
  }

  @GetMapping("/fee-categories")
  @PreAuthorize("hasAuthority('course:read')")
  public List<FeeCategoryView> listCategories() {
    return academicCatalogService.listCategories();
  }

  @PostMapping("/fee-categories")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('course:write')")
  public FeeCategoryView createCategory(@Valid @RequestBody CreateCategoryRequest request) {
    return academicCatalogService.createCategory(request.code(), request.name());
  }

  @GetMapping("/courses")
  @PreAuthorize("hasAuthority('course:read')")
  public PageResponse<CourseView> listCourses(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return academicCatalogService.listCourses(q, page, size);
  }

  @GetMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('course:read')")
  public CourseView getCourse(@PathVariable("id") Long id) {
    return academicCatalogService.getCourse(id);
  }

  @PostMapping("/courses")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('course:write')")
  public CourseView createCourse(@Valid @RequestBody CreateCourseRequest request) {
    return academicCatalogService.createCourse(request.code(), request.name(), request.description());
  }

  @PutMapping("/courses/{id}")
  @PreAuthorize("hasAuthority('course:write')")
  public CourseView updateCourse(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateCourseRequest request) {
    return academicCatalogService.updateCourse(id, request.name(), request.description());
  }

  @PostMapping("/courses/{id}/deactivate")
  @PreAuthorize("hasAuthority('course:write')")
  public CourseView deactivateCourse(@PathVariable("id") Long id) {
    return academicCatalogService.deactivateCourse(id);
  }

  @GetMapping("/courses/{courseId}/fee-plans")
  @PreAuthorize("hasAuthority('course:read')")
  public List<FeePlanView> listFeePlans(@PathVariable("courseId") Long courseId) {
    return academicCatalogService.listFeePlans(courseId);
  }

  @GetMapping("/fee-plans/{id}")
  @PreAuthorize("hasAuthority('course:read')")
  public FeePlanView getFeePlan(@PathVariable("id") Long id) {
    return academicCatalogService.getFeePlan(id);
  }

  @PostMapping("/courses/{courseId}/fee-plans")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('course:write')")
  public FeePlanView createFeePlan(
      @PathVariable("courseId") Long courseId, @Valid @RequestBody CreateFeePlanRequest request) {
    return academicCatalogService.createFeePlan(
        courseId,
        request.code(),
        request.name(),
        request.currency(),
        request.totalAmount(),
        mapInstallments(request.installments()));
  }

  @PutMapping("/fee-plans/{id}")
  @PreAuthorize("hasAuthority('course:write')")
  public FeePlanView updateFeePlan(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateFeePlanRequest request) {
    return academicCatalogService.replaceFeePlanInstallments(
        id, request.name(), request.totalAmount(), mapInstallments(request.installments()));
  }

  @GetMapping("/batches")
  @PreAuthorize("hasAuthority('course:read')")
  public List<BatchView> listBatches(
      @RequestParam(name = "courseId", required = false) Long courseId) {
    return academicCatalogService.listBatches(courseId);
  }

  @GetMapping("/batches/{id}")
  @PreAuthorize("hasAuthority('course:read')")
  public BatchView getBatch(@PathVariable("id") Long id) {
    return academicCatalogService.getBatch(id);
  }

  @PostMapping("/batches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('course:write')")
  public BatchView createBatch(@Valid @RequestBody CreateBatchRequest request) {
    return academicCatalogService.createBatch(
        request.courseId(),
        request.academicYearId(),
        request.code(),
        request.name(),
        request.capacity(),
        request.startDate(),
        request.endDate());
  }

  @PutMapping("/batches/{id}")
  @PreAuthorize("hasAuthority('course:write')")
  public BatchView updateBatch(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateBatchRequest request) {
    return academicCatalogService.updateBatch(
        id, request.name(), request.capacity(), request.status(), request.startDate(), request.endDate());
  }

  private static List<InstallmentInput> mapInstallments(List<InstallmentRequest> installments) {
    return installments.stream()
        .map(
            i ->
                new InstallmentInput(
                    i.seq(), i.feeCategoryId(), i.label(), i.amount(), i.dueOffsetDays()))
        .toList();
  }
}

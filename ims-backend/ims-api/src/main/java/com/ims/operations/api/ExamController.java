package com.ims.operations.api;

import com.ims.operations.api.ExamDtos.BatchExamResultsSummary;
import com.ims.operations.api.ExamDtos.ExamComponentResponse;
import com.ims.operations.api.ExamDtos.ExamResponse;
import com.ims.operations.api.ExamDtos.ExamResultResponse;
import com.ims.operations.application.ExamService;
import com.ims.operations.application.ExamService.ComponentMarkInput;
import com.ims.operations.application.ExamService.CreateComponentCommand;
import com.ims.operations.application.ExamService.CreateExamCommand;
import com.ims.operations.application.ExamService.UpdateExamCommand;
import com.ims.operations.application.ExamService.UpsertResultCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
@Tag(name = "Exams", description = "Exams, components, marks, publish/lock")
@SecurityRequirement(name = "bearer-jwt")
public class ExamController {

  private final ExamService examService;

  public ExamController(ExamService examService) {
    this.examService = examService;
  }

  public record CreateExamRequest(
      @NotNull Long batchId,
      @NotBlank @Size(max = 128) String name,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
      Long originalExamId) {}

  public record UpdateExamRequest(
      @NotBlank @Size(max = 128) String name,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate) {}

  public record CreateComponentRequest(
      Long subjectId,
      @NotBlank @Size(max = 32) String code,
      @NotBlank @Size(max = 128) String name,
      @NotNull @DecimalMin("0.0") BigDecimal maxMarks,
      @NotNull @DecimalMin("0.0") BigDecimal passMarks,
      BigDecimal weightage,
      Integer seq) {}

  public record ComponentMarkRequest(@NotNull Long componentId, BigDecimal marks) {}

  public record UpsertResultRequest(
      @NotNull Long studentId,
      @Size(max = 32) String resultStatus,
      BigDecimal graceMarks,
      @Size(max = 8) String grade,
      @Size(max = 255) String remarks,
      List<@Valid ComponentMarkRequest> componentMarks) {}

  public record UpsertResultsRequest(@NotEmpty List<@Valid UpsertResultRequest> results) {}

  public record UnlockRequest(@NotBlank @Size(min = 5, max = 512) String reason) {}

  @GetMapping("/exam-grades")
  @Operation(summary = "List predefined exam grades")
  @PreAuthorize(
      "hasAuthority('exam:mark') or hasAuthority('exam:publish') or hasAuthority('course:read') or hasRole('STUDENT')")
  public List<String> listGrades() {
    return examService.listGrades();
  }

  @PostMapping("/exams")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create exam (SCHEDULED)")
  @PreAuthorize("hasAuthority('exam:mark')")
  public ExamResponse create(@Valid @RequestBody CreateExamRequest request) {
    return examService.create(
        new CreateExamCommand(
            request.batchId(), request.name(), request.examDate(), request.originalExamId()));
  }

  @GetMapping("/exams")
  @Operation(summary = "List exams")
  @PreAuthorize(
      "hasAuthority('exam:mark') or hasAuthority('exam:publish') or hasAuthority('course:read') or hasRole('STUDENT')")
  public List<ExamResponse> list(@RequestParam(required = false) Long batchId) {
    return examService.list(batchId);
  }

  @GetMapping("/exams/{id}")
  @Operation(summary = "Get exam with components")
  @PreAuthorize(
      "hasAuthority('exam:mark') or hasAuthority('exam:publish') or hasAuthority('course:read') or hasRole('STUDENT')")
  public ExamResponse get(@PathVariable("id") Long id) {
    return examService.get(id);
  }

  @PutMapping("/exams/{id}")
  @Operation(summary = "Update SCHEDULED exam")
  @PreAuthorize("hasAuthority('exam:mark')")
  public ExamResponse update(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateExamRequest request) {
    return examService.update(id, new UpdateExamCommand(request.name(), request.examDate()));
  }

  @PostMapping("/exams/{id}/components")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add exam component")
  @PreAuthorize("hasAuthority('exam:mark')")
  public ExamComponentResponse addComponent(
      @PathVariable("id") Long id, @Valid @RequestBody CreateComponentRequest request) {
    return examService.addComponent(
        id,
        new CreateComponentCommand(
            request.subjectId(),
            request.code(),
            request.name(),
            request.maxMarks(),
            request.passMarks(),
            request.weightage(),
            request.seq()));
  }

  @PostMapping("/exams/{id}/open-marks")
  @Operation(summary = "Open exam for marks entry (DRAFT_MARKS)")
  @PreAuthorize("hasAuthority('exam:mark')")
  public ExamResponse openMarks(@PathVariable("id") Long id) {
    return examService.openMarks(id);
  }

  @PutMapping("/exams/{id}/results")
  @Operation(summary = "Upsert student results / component marks")
  @PreAuthorize("hasAuthority('exam:mark')")
  public List<ExamResultResponse> upsertResults(
      @PathVariable("id") Long id, @Valid @RequestBody UpsertResultsRequest request) {
    List<UpsertResultCommand> commands =
        request.results().stream()
            .map(
                r ->
                    new UpsertResultCommand(
                        r.studentId(),
                        r.resultStatus(),
                        r.graceMarks(),
                        r.grade(),
                        r.remarks(),
                        r.componentMarks() == null
                            ? List.of()
                            : r.componentMarks().stream()
                                .map(m -> new ComponentMarkInput(m.componentId(), m.marks()))
                                .toList()))
            .toList();
    return examService.upsertResults(id, commands);
  }

  @GetMapping("/exams/{id}/results")
  @Operation(summary = "List results (students see published only)")
  @PreAuthorize(
      "hasAuthority('exam:mark') or hasAuthority('exam:publish') or hasRole('STUDENT')")
  public List<ExamResultResponse> listResults(@PathVariable("id") Long id) {
    return examService.listResults(id);
  }

  @PostMapping("/exams/{id}/publish")
  @Operation(summary = "Publish results")
  @PreAuthorize("hasAuthority('exam:publish')")
  public ExamResponse publish(@PathVariable("id") Long id) {
    return examService.publish(id);
  }

  @PostMapping("/exams/{id}/lock")
  @Operation(summary = "Lock published results")
  @PreAuthorize("hasAuthority('exam:publish')")
  public ExamResponse lock(@PathVariable("id") Long id) {
    return examService.lock(id);
  }

  @PostMapping("/exams/{id}/unlock")
  @Operation(summary = "Unlock for corrections (audited)")
  @PreAuthorize("hasAuthority('exam:publish')")
  public ExamResponse unlock(
      @PathVariable("id") Long id, @Valid @RequestBody UnlockRequest request) {
    return examService.unlock(id, request.reason());
  }

  @GetMapping("/exams/batches/{batchId}/summary")
  @Operation(summary = "Batch exam results summary (RP-EX-01)")
  @PreAuthorize("hasAuthority('exam:mark') or hasAuthority('exam:publish')")
  public BatchExamResultsSummary batchSummary(
      @PathVariable("batchId") Long batchId, @RequestParam Long examId) {
    return examService.batchSummary(batchId, examId);
  }
}

package com.ims.imports.api;

import com.ims.imports.api.MarksImportDtos.MarksImportJobResponse;
import com.ims.imports.application.MarksBulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bulk marks import", description = "CSV marks upload with job status (FR-IE-02)")
@SecurityRequirement(name = "bearer-jwt")
public class MarksBulkImportController {

  private final MarksBulkImportService marksBulkImportService;

  public MarksBulkImportController(MarksBulkImportService marksBulkImportService) {
    this.marksBulkImportService = marksBulkImportService;
  }

  @GetMapping(value = "/exams/{examId}/marks-import/template", produces = "text/csv")
  @Operation(summary = "Download marks CSV template for an exam (component columns included)")
  @PreAuthorize("hasAuthority('exam:mark')")
  public ResponseEntity<byte[]> template(@PathVariable Long examId) {
    String csv = marksBulkImportService.templateCsv(examId);
    byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"exam-" + examId + "-marks-template.csv\"")
        .contentType(new MediaType("text", "csv"))
        .body(bytes);
  }

  @PostMapping(
      value = "/exams/{examId}/marks-import",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary =
          "Upload marks CSV (idempotent upsert per exam+student; async when rows > sync threshold)")
  @PreAuthorize("hasAuthority('exam:mark')")
  public MarksImportJobResponse upload(
      @PathVariable Long examId, @RequestPart("file") MultipartFile file) {
    return marksBulkImportService.start(examId, file);
  }

  @GetMapping("/marks-import-jobs/{jobId}")
  @Operation(summary = "Get marks import job status and error report")
  @PreAuthorize("hasAuthority('exam:mark')")
  public MarksImportJobResponse job(@PathVariable Long jobId) {
    return marksBulkImportService.getJob(jobId);
  }
}

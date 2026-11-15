package com.ims.imports.api;

import com.ims.imports.api.ImportDtos.ImportResultResponse;
import com.ims.imports.application.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "CSV import", description = "Bulk student and faculty CSV import")
@SecurityRequirement(name = "bearer-jwt")
public class CsvImportController {

  private final CsvImportService csvImportService;

  public CsvImportController(CsvImportService csvImportService) {
    this.csvImportService = csvImportService;
  }

  @GetMapping(value = "/students/template", produces = "text/csv")
  @Operation(summary = "Download student CSV template")
  @PreAuthorize("hasAuthority('import:write') and hasAuthority('student:write')")
  public ResponseEntity<byte[]> studentTemplate() {
    return csvAttachment("students-import-template.csv", CsvImportService.STUDENT_TEMPLATE);
  }

  @GetMapping(value = "/faculties/template", produces = "text/csv")
  @Operation(summary = "Download faculty CSV template")
  @PreAuthorize("hasAuthority('import:write') and hasAuthority('faculty:write')")
  public ResponseEntity<byte[]> facultyTemplate() {
    return csvAttachment("faculties-import-template.csv", CsvImportService.FACULTY_TEMPLATE);
  }

  @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Import students from CSV (validate then insert; error report included)")
  @PreAuthorize("hasAuthority('import:write') and hasAuthority('student:write')")
  public ImportResultResponse importStudents(@RequestPart("file") MultipartFile file) {
    return csvImportService.importStudents(file);
  }

  @PostMapping(value = "/faculties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Import faculties from CSV (validate then insert; error report included)")
  @PreAuthorize("hasAuthority('import:write') and hasAuthority('faculty:write')")
  public ImportResultResponse importFaculties(@RequestPart("file") MultipartFile file) {
    return csvImportService.importFaculties(file);
  }

  private static ResponseEntity<byte[]> csvAttachment(String fileName, String body) {
    byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .contentType(new MediaType("text", "csv"))
        .body(bytes);
  }
}

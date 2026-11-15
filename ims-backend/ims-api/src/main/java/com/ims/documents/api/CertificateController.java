package com.ims.documents.api;

import com.ims.documents.api.CertificateDtos.CertificateDownloadResponse;
import com.ims.documents.api.CertificateDtos.CertificateTemplateResponse;
import com.ims.documents.api.CertificateDtos.GeneratedDocumentResponse;
import com.ims.documents.application.CertificateService;
import com.ims.documents.application.CertificateService.GenerateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Certificates", description = "Bonafide / completion certificate generation")
@SecurityRequirement(name = "bearer-jwt")
public class CertificateController {

  private final CertificateService certificateService;

  public CertificateController(CertificateService certificateService) {
    this.certificateService = certificateService;
  }

  public record GenerateCertificateRequest(
      @NotNull Long studentId,
      @NotBlank @Size(max = 32) String templateCode,
      Long enrollmentId) {}

  @GetMapping("/certificate-templates")
  @Operation(summary = "List active certificate templates")
  @PreAuthorize(
      "hasAuthority('certificate:generate') or hasAuthority('certificate:read') or hasRole('STUDENT')")
  public List<CertificateTemplateResponse> listTemplates() {
    return certificateService.listTemplates();
  }

  @PostMapping("/certificates/generate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Generate a certificate PDF and store under tenant prefix")
  @PreAuthorize("hasAuthority('certificate:generate')")
  public GeneratedDocumentResponse generate(@Valid @RequestBody GenerateCertificateRequest request) {
    return certificateService.generate(
        new GenerateCommand(request.studentId(), request.templateCode(), request.enrollmentId()));
  }

  @GetMapping("/students/{studentId}/certificates")
  @Operation(summary = "List certificates for a student")
  @PreAuthorize(
      "hasAuthority('certificate:read') or hasAuthority('certificate:generate') or hasRole('STUDENT')")
  public List<GeneratedDocumentResponse> listForStudent(
      @PathVariable("studentId") Long studentId) {
    return certificateService.listForStudent(studentId);
  }

  @GetMapping("/certificates/{id}/download")
  @Operation(summary = "Get a time-limited signed download URL")
  @PreAuthorize(
      "hasAuthority('certificate:read') or hasAuthority('certificate:generate') or hasRole('STUDENT')")
  public CertificateDownloadResponse download(@PathVariable("id") Long id) {
    return certificateService.download(id);
  }
}

package com.ims.people.document.api;

import com.ims.people.document.application.PersonDocumentService;
import com.ims.people.document.application.PersonDocumentService.InitUploadCommand;
import com.ims.people.document.domain.DocumentOwnerType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/v1/people/{ownerType}/{ownerId}/documents")
@Tag(name = "Person documents", description = "Private identity documents via pre-signed object storage")
@SecurityRequirement(name = "bearer-jwt")
public class PersonDocumentController {

  private final PersonDocumentService personDocumentService;

  public PersonDocumentController(PersonDocumentService personDocumentService) {
    this.personDocumentService = personDocumentService;
  }

  public record InitUploadRequest(
      @NotBlank @Size(max = 64) String docType,
      @NotBlank @Size(max = 255) String fileName,
      @NotBlank @Size(max = 128) String contentType,
      @Positive long fileSize) {}

  public record CompleteUploadRequest(@Size(max = 128) String checksum) {}

  @GetMapping
  @Operation(summary = "List active person documents")
  @PreAuthorize("hasAuthority('document:read') or hasRole('ADMIN')")
  public List<PersonDocumentResponse> list(
      @PathVariable("ownerType") DocumentOwnerType ownerType,
      @PathVariable("ownerId") Long ownerId) {
    return personDocumentService.list(ownerType, ownerId);
  }

  @PostMapping("/uploads")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create pending metadata and return a pre-signed PUT URL")
  @PreAuthorize("hasAuthority('document:write') or hasRole('ADMIN')")
  public PresignedUploadResponse initUpload(
      @PathVariable("ownerType") DocumentOwnerType ownerType,
      @PathVariable("ownerId") Long ownerId,
      @Valid @RequestBody InitUploadRequest request) {
    return personDocumentService.initUpload(
        ownerType,
        ownerId,
        new InitUploadCommand(
            request.docType(), request.fileName(), request.contentType(), request.fileSize()));
  }

  @PostMapping("/{documentId}/complete")
  @Operation(summary = "Mark a document ACTIVE after the client PUT to object storage")
  @PreAuthorize("hasAuthority('document:write') or hasRole('ADMIN')")
  public PersonDocumentResponse complete(
      @PathVariable("ownerType") DocumentOwnerType ownerType,
      @PathVariable("ownerId") Long ownerId,
      @PathVariable("documentId") Long documentId,
      @RequestBody(required = false) CompleteUploadRequest request) {
    String checksum = request == null ? null : request.checksum();
    return personDocumentService.complete(ownerType, ownerId, documentId, checksum);
  }

  @GetMapping("/photo")
  @Operation(summary = "Latest profile photo as a time-limited inline URL")
  @PreAuthorize("hasAuthority('document:read') or hasRole('ADMIN')")
  public PresignedDownloadResponse photo(
      @PathVariable("ownerType") DocumentOwnerType ownerType,
      @PathVariable("ownerId") Long ownerId) {
    return personDocumentService.latestPhoto(ownerType, ownerId);
  }

  @GetMapping("/{documentId}/download")
  @Operation(summary = "Return a time-limited pre-signed GET URL")
  @PreAuthorize("hasAuthority('document:read') or hasRole('ADMIN')")
  public PresignedDownloadResponse download(
      @PathVariable("ownerType") DocumentOwnerType ownerType,
      @PathVariable("ownerId") Long ownerId,
      @PathVariable("documentId") Long documentId) {
    return personDocumentService.download(ownerType, ownerId, documentId);
  }

  @PostMapping("/{documentId}/delete")
  @Operation(summary = "Soft-delete a person document")
  @PreAuthorize("hasAuthority('document:write') or hasRole('ADMIN')")
  public PersonDocumentResponse delete(
      @PathVariable("ownerType") DocumentOwnerType ownerType,
      @PathVariable("ownerId") Long ownerId,
      @PathVariable("documentId") Long documentId) {
    return personDocumentService.delete(ownerType, ownerId, documentId);
  }
}

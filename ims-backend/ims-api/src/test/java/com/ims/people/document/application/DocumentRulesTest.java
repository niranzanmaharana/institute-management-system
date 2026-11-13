package com.ims.people.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ims.people.document.domain.DocumentOwnerType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DocumentRulesTest {

  @Test
  void rejectsOversizedFile() {
    assertThatThrownBy(() -> DocumentRules.validateFileSize(5 * 1024 * 1024 + 1, 5 * 1024 * 1024))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("maximum size");
  }

  @Test
  void rejectsDisallowedContentType() {
    assertThatThrownBy(() -> DocumentRules.normalizeContentType("application/zip"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Allowed types");
  }

  @Test
  void normalizesJpegAliasAndBuildsTenantPrefixedKey() {
    assertThat(DocumentRules.normalizeContentType("image/jpg")).isEqualTo("image/jpeg");
    String key =
        DocumentRules.storageKey(12L, DocumentOwnerType.STUDENT, 44L, "id.pdf");
    assertThat(key).startsWith("institutes/12/student/44/");
    assertThat(key).endsWith("/id.pdf");
  }

  @Test
  void stripsPathFromFileName() {
    assertThat(DocumentRules.sanitizeFileName("..\\secret\\aadhaar.pdf")).isEqualTo("aadhaar.pdf");
  }

  @Test
  void photoMustBeJpegOrPng() {
    assertThatThrownBy(() -> DocumentRules.validatePhotoContentType("PHOTO", "application/pdf"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("JPEG or PNG");
  }
}

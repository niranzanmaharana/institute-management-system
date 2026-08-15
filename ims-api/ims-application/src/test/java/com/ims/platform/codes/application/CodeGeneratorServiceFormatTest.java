package com.ims.platform.codes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.ims.platform.codes.domain.CodeEntityType;
import org.junit.jupiter.api.Test;

class CodeGeneratorServiceFormatTest {

  @Test
  void formatsStudentAsInstituteIdPlusSixDigits() {
    assertThat(CodeGeneratorService.format(CodeEntityType.STUDENT, 1L, 1L)).isEqualTo("1000001");
    assertThat(CodeGeneratorService.format(CodeEntityType.STUDENT, 1L, 2L)).isEqualTo("1000002");
    assertThat(CodeGeneratorService.format(CodeEntityType.STUDENT, 12L, 1L)).isEqualTo("12000001");
  }

  @Test
  void formatsPrefixedCatalogCodes() {
    assertThat(CodeGeneratorService.format(CodeEntityType.COURSE, 1L, 1L)).isEqualTo("C1-00001");
    assertThat(CodeGeneratorService.format(CodeEntityType.FEE_PLAN, 1L, 3L)).isEqualTo("P1-00003");
    assertThat(CodeGeneratorService.format(CodeEntityType.BATCH, 2L, 10L)).isEqualTo("B2-00010");
    assertThat(CodeGeneratorService.format(CodeEntityType.ACADEMIC_YEAR, 1L, 1L))
        .isEqualTo("AY1-0001");
    assertThat(CodeGeneratorService.format(CodeEntityType.FEE_CATEGORY, 1L, 1L))
        .isEqualTo("FC1-0001");
    assertThat(CodeGeneratorService.format(CodeEntityType.ADMISSION, 1L, 1L))
        .isEqualTo("A1-000001");
    assertThat(CodeGeneratorService.format(CodeEntityType.FACULTY, 1L, 1L)).isEqualTo("F1-00001");
    assertThat(CodeGeneratorService.format(CodeEntityType.STAFF, 1L, 1L)).isEqualTo("S1-00001");
    assertThat(CodeGeneratorService.format(CodeEntityType.INSTITUTE, 0L, 1L)).isEqualTo("I00001");
  }
}

package com.ims.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

  @Test
  void shouldBuildErrorWithDetails() {
    ApiError error =
        ApiError.of(
            400,
            "VALIDATION_ERROR",
            "Invalid request",
            "trace-1",
            List.of(new ApiError.FieldErrorDetail("email", "must be valid")));

    assertThat(error.status()).isEqualTo(400);
    assertThat(error.traceId()).isEqualTo("trace-1");
    assertThat(error.details()).hasSize(1);
    assertThat(error.timestamp()).isNotNull();
  }
}

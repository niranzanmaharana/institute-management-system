package com.ims.common.tracing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdsTest {

  @AfterEach
  void clear() {
    CorrelationIds.clearAllCorrelation();
  }

  @Test
  void returnsDashWhenMissing() {
    assertThat(CorrelationIds.traceId()).isEqualTo("-");
    assertThat(CorrelationIds.spanId()).isEqualTo("-");
    assertThat(CorrelationIds.requestId()).isEqualTo("-");
  }

  @Test
  void readsMdcValues() {
    MDC.put(CorrelationIds.TRACE_ID, "t1");
    MDC.put(CorrelationIds.SPAN_ID, "s1");
    CorrelationIds.putRequestId("r1");
    assertThat(CorrelationIds.traceId()).isEqualTo("t1");
    assertThat(CorrelationIds.spanId()).isEqualTo("s1");
    assertThat(CorrelationIds.requestId()).isEqualTo("r1");
  }
}

package com.ims.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void shouldNormalizeToTwoDecimalPlacesHalfUp() {
    assertThat(Money.normalize(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
    assertThat(Money.of("1.2")).isEqualByComparingTo("1.20");
  }
}

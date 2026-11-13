package com.ims.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

  public static final int SCALE = 2;
  public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private Money() {}

  public static BigDecimal of(String amount) {
    return new BigDecimal(amount).setScale(SCALE, ROUNDING);
  }

  public static BigDecimal normalize(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("amount must not be null");
    }
    return amount.setScale(SCALE, ROUNDING);
  }
}

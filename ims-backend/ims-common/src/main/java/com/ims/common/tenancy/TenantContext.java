package com.ims.common.tenancy;

/**
 * Request-scoped tenant holder. Populated from JWT in later phases; tests set it explicitly.
 */
public final class TenantContext {

  private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static void setInstituteId(Long instituteId) {
    CURRENT.set(instituteId);
  }

  public static Long getInstituteId() {
    return CURRENT.get();
  }

  public static long requireInstituteId() {
    Long id = CURRENT.get();
    if (id == null) {
      throw new IllegalStateException("TenantContext is not set");
    }
    return id;
  }

  public static void clear() {
    CURRENT.remove();
  }
}

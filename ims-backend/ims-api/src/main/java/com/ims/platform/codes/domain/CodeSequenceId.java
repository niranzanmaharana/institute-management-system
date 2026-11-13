package com.ims.platform.codes.domain;

import java.io.Serializable;
import java.util.Objects;

public class CodeSequenceId implements Serializable {

  private Long instituteId;
  private CodeEntityType entityType;

  public CodeSequenceId() {}

  public CodeSequenceId(Long instituteId, CodeEntityType entityType) {
    this.instituteId = instituteId;
    this.entityType = entityType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CodeSequenceId that)) {
      return false;
    }
    return Objects.equals(instituteId, that.instituteId) && entityType == that.entityType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instituteId, entityType);
  }
}

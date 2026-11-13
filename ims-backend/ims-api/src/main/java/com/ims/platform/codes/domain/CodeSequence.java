package com.ims.platform.codes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_sequences")
@IdClass(CodeSequenceId.class)
public class CodeSequence {

  @Id
  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 32)
  private CodeEntityType entityType;

  @Column(name = "next_value", nullable = false)
  private long nextValue;

  protected CodeSequence() {}

  public static CodeSequence start(long instituteId, CodeEntityType entityType, long firstValue) {
    CodeSequence seq = new CodeSequence();
    seq.instituteId = instituteId;
    seq.entityType = entityType;
    seq.nextValue = firstValue;
    return seq;
  }

  public long allocateNext() {
    long allocated = nextValue;
    nextValue = nextValue + 1;
    return allocated;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public CodeEntityType getEntityType() {
    return entityType;
  }

  public long getNextValue() {
    return nextValue;
  }
}

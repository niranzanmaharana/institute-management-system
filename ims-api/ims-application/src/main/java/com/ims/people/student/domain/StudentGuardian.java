package com.ims.people.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "student_guardians")
@IdClass(StudentGuardian.Pk.class)
public class StudentGuardian {

  @Id
  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Id
  @Column(name = "guardian_id", nullable = false)
  private Long guardianId;

  @Column(name = "institute_id", nullable = false)
  private Long instituteId;

  @Column(nullable = false, length = 64)
  private String relation;

  @Column(name = "is_primary", nullable = false)
  private boolean primaryGuardian;

  protected StudentGuardian() {}

  public StudentGuardian(
      Long instituteId, Long studentId, Long guardianId, String relation, boolean primaryGuardian) {
    this.instituteId = instituteId;
    this.studentId = studentId;
    this.guardianId = guardianId;
    this.relation = relation.trim();
    this.primaryGuardian = primaryGuardian;
  }

  public Long getStudentId() {
    return studentId;
  }

  public Long getGuardianId() {
    return guardianId;
  }

  public Long getInstituteId() {
    return instituteId;
  }

  public String getRelation() {
    return relation;
  }

  public boolean isPrimaryGuardian() {
    return primaryGuardian;
  }

  public static class Pk implements Serializable {
    private Long studentId;
    private Long guardianId;

    public Pk() {}

    public Pk(Long studentId, Long guardianId) {
      this.studentId = studentId;
      this.guardianId = guardianId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Pk pk)) {
        return false;
      }
      return Objects.equals(studentId, pk.studentId) && Objects.equals(guardianId, pk.guardianId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(studentId, guardianId);
    }
  }
}

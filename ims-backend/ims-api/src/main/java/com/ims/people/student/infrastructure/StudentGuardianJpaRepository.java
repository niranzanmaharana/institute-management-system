package com.ims.people.student.infrastructure;

import com.ims.people.student.domain.StudentGuardian;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGuardianJpaRepository
    extends JpaRepository<StudentGuardian, StudentGuardian.Pk> {

  List<StudentGuardian> findByStudentIdAndInstituteId(Long studentId, Long instituteId);

  void deleteByStudentIdAndInstituteId(Long studentId, Long instituteId);
}

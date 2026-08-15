package com.ims.people.student.infrastructure;

import com.ims.people.student.domain.StudentAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentAddressJpaRepository extends JpaRepository<StudentAddress, Long> {

  List<StudentAddress> findByStudentIdAndInstituteId(Long studentId, Long instituteId);

  void deleteByStudentIdAndInstituteId(Long studentId, Long instituteId);
}

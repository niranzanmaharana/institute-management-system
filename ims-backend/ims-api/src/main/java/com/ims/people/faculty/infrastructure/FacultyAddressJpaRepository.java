package com.ims.people.faculty.infrastructure;

import com.ims.people.faculty.domain.FacultyAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyAddressJpaRepository extends JpaRepository<FacultyAddress, Long> {

  List<FacultyAddress> findByFacultyIdAndInstituteId(Long facultyId, Long instituteId);

  void deleteByFacultyIdAndInstituteId(Long facultyId, Long instituteId);
}

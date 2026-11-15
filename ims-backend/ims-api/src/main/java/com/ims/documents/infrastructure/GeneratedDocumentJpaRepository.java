package com.ims.documents.infrastructure;

import com.ims.documents.domain.GeneratedDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedDocumentJpaRepository extends JpaRepository<GeneratedDocument, Long> {

  List<GeneratedDocument> findByInstituteIdAndStudentIdOrderByIssuedAtDescIdDesc(
      Long instituteId, Long studentId);

  Optional<GeneratedDocument> findByIdAndInstituteId(Long id, Long instituteId);
}

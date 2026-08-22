package com.ims.people.document.infrastructure;

import com.ims.people.document.domain.DocumentOwnerType;
import com.ims.people.document.domain.PersonDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonDocumentJpaRepository extends JpaRepository<PersonDocument, Long> {

  List<PersonDocument> findByInstituteIdAndOwnerTypeAndOwnerIdAndStatusOrderByUploadedAtDesc(
      Long instituteId, DocumentOwnerType ownerType, Long ownerId, String status);

  Optional<PersonDocument> findByIdAndInstituteId(Long id, Long instituteId);

  Optional<PersonDocument>
      findFirstByInstituteIdAndOwnerTypeAndOwnerIdAndDocTypeAndStatusOrderByUploadedAtDesc(
          Long instituteId,
          DocumentOwnerType ownerType,
          Long ownerId,
          String docType,
          String status);
}

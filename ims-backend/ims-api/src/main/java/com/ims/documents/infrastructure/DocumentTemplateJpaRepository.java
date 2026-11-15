package com.ims.documents.infrastructure;

import com.ims.documents.domain.DocumentTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateJpaRepository extends JpaRepository<DocumentTemplate, Long> {

  List<DocumentTemplate> findByActiveTrueOrderByCodeAsc();

  Optional<DocumentTemplate> findByCodeIgnoreCase(String code);
}

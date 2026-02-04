package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    List<ApplicationDocument> findByApplicationId(Long applicationId);
    Optional<ApplicationDocument> findByApplicationIdAndDocumentType(Long applicationId, String documentType);
}

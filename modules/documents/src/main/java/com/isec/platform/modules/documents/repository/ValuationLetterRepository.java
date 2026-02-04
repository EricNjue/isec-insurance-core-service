package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ValuationLetterRepository extends JpaRepository<ValuationLetter, Long> {
    List<ValuationLetter> findByPolicyId(Long policyId);
    Optional<ValuationLetter> findFirstByPolicyIdAndGeneratedAtAfter(Long policyId, LocalDateTime generatedAt);
}

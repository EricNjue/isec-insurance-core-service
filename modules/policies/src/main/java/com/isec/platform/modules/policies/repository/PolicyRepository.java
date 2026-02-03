package com.isec.platform.modules.policies.repository;

import com.isec.platform.modules.policies.domain.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByApplicationId(Long applicationId);
    Optional<Policy> findByPolicyNumber(String policyNumber);
}

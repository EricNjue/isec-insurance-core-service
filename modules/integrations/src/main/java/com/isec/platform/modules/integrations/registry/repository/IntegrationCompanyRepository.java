package com.isec.platform.modules.integrations.registry.repository;

import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationCompanyRepository extends JpaRepository<IntegrationCompany, Long> {
    Optional<IntegrationCompany> findByCode(String code);
    boolean existsByCode(String code);
    List<IntegrationCompany> findAllByActiveTrue();
}

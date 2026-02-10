package com.isec.platform.modules.rating.repository;

import com.isec.platform.modules.rating.domain.RateBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateBookRepository extends JpaRepository<RateBook, Long> {

    @Query("SELECT rb FROM RateBook rb WHERE rb.tenantId = :tenantId AND rb.active = true")
    Optional<RateBook> findActiveByTenantId(@Param("tenantId") String tenantId);

    List<RateBook> findAllByTenantId(String tenantId);
}

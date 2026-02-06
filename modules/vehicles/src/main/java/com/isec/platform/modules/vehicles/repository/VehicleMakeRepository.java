package com.isec.platform.modules.vehicles.repository;

import com.isec.platform.modules.vehicles.domain.VehicleMake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleMakeRepository extends JpaRepository<VehicleMake, UUID>, JpaSpecificationExecutor<VehicleMake> {
    Optional<VehicleMake> findByCode(String code);
    boolean existsByCode(String code);
}

package com.isec.platform.modules.vehicles.repository;

import com.isec.platform.modules.vehicles.domain.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, UUID>, JpaSpecificationExecutor<VehicleModel> {
    Optional<VehicleModel> findByMakeIdAndCode(UUID makeId, String code);
    boolean existsByMakeIdAndCode(UUID makeId, String code);
    List<VehicleModel> findByMakeId(UUID makeId);
    List<VehicleModel> findByMakeCode(String makeCode);
    boolean existsByMakeIdAndActiveTrue(UUID makeId);
}

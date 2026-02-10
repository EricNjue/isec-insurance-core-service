package com.isec.platform.modules.vehicles.repository;

import com.isec.platform.modules.vehicles.domain.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVehicleRepository extends JpaRepository<UserVehicle, UUID> {
    List<UserVehicle> findAllByUserId(String userId);
    Optional<UserVehicle> findByRegistrationNumberAndUserId(String registrationNumber, String userId);
}

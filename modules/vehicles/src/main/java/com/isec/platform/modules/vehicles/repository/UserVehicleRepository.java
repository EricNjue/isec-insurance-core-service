package com.isec.platform.modules.vehicles.repository;

import com.isec.platform.modules.vehicles.domain.UserVehicle;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserVehicleRepository extends ReactiveCrudRepository<UserVehicle, UUID> {
    Flux<UserVehicle> findAllByUserId(String userId);
    Mono<UserVehicle> findByRegistrationNumberAndUserId(String registrationNumber, String userId);
}

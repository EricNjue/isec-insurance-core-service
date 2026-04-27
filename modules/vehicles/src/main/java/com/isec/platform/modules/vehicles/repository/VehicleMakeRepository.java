package com.isec.platform.modules.vehicles.repository;

import com.isec.platform.modules.vehicles.domain.VehicleMake;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VehicleMakeRepository extends ReactiveCrudRepository<VehicleMake, UUID> {
    Mono<VehicleMake> findByCode(String code);
    Mono<Boolean> existsByCode(String code);
    Flux<VehicleMake> findAllByActiveTrue();
}

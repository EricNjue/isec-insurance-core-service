package com.isec.platform.modules.vehicles.repository;

import com.isec.platform.modules.vehicles.domain.VehicleModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface VehicleModelRepository extends ReactiveCrudRepository<VehicleModel, UUID> {
    Mono<VehicleModel> findByMakeIdAndCode(UUID makeId, String code);
    Mono<Boolean> existsByMakeIdAndCode(UUID makeId, String code);
    Flux<VehicleModel> findByMakeId(UUID makeId);
    Flux<VehicleModel> findByMakeIdAndActiveTrue(UUID makeId);
    Flux<VehicleModel> findAllByActiveTrue();
    Mono<Boolean> existsByMakeIdAndActiveTrue(UUID makeId);
}

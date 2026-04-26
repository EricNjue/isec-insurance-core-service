package com.isec.platform.modules.vehicles.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.vehicles.domain.VehicleMake;
import com.isec.platform.modules.vehicles.domain.VehicleModel;
import com.isec.platform.modules.vehicles.dto.VehicleMakeRequest;
import com.isec.platform.modules.vehicles.dto.VehicleMakeResponse;
import com.isec.platform.modules.vehicles.dto.VehicleModelRequest;
import com.isec.platform.modules.vehicles.dto.VehicleModelResponse;
import com.isec.platform.modules.vehicles.repository.VehicleMakeRepository;
import com.isec.platform.modules.vehicles.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleMakeRepository makeRepository;
    private final VehicleModelRepository modelRepository;

    public Mono<VehicleMakeResponse> createMake(VehicleMakeRequest request) {
        return makeRepository.existsByCode(request.getCode())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("Vehicle make with code " + request.getCode() + " already exists"));
                    }
                    VehicleMake make = VehicleMake.builder()
                            .id(UUID.randomUUID())
                            .code(request.getCode().toUpperCase())
                            .name(request.getName())
                            .country(request.getCountry())
                            .active(request.isActive())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return makeRepository.save(make).map(this::mapToMakeResponse);
                });
    }

    public Mono<VehicleMakeResponse> updateMake(UUID id, VehicleMakeRequest request) {
        return makeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleMake", id)))
                .flatMap(make -> {
                    make.setName(request.getName());
                    make.setCountry(request.getCountry());
                    make.setActive(request.isActive());
                    make.setUpdatedAt(LocalDateTime.now());
                    
                    Mono<VehicleMake> saveMake = makeRepository.save(make);
                    
                    if (!request.isActive()) {
                        return deactivateModelsForMake(id).then(saveMake);
                    }
                    return saveMake;
                })
                .map(this::mapToMakeResponse);
    }

    public Mono<VehicleMakeResponse> getMake(UUID id) {
        return makeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleMake", id)))
                .map(this::mapToMakeResponse);
    }

    public Flux<VehicleMakeResponse> getAllMakes(Boolean active) {
        Flux<VehicleMake> makes = (active != null && active) ? makeRepository.findAllByActiveTrue() : makeRepository.findAll();
        return makes.map(this::mapToMakeResponse);
    }

    public Flux<VehicleModelResponse> getAllModels(UUID makeId, String makeCode, Boolean active) {
        Flux<VehicleModel> models;
        if (makeId != null) {
            models = active != null && active ? modelRepository.findByMakeIdAndActiveTrue(makeId) : modelRepository.findByMakeId(makeId);
        } else if (makeCode != null) {
            models = active != null && active ? modelRepository.findByMakeCodeAndActiveTrue(makeCode) : modelRepository.findByMakeCode(makeCode);
        } else {
            models = active != null && active ? modelRepository.findAllByActiveTrue() : modelRepository.findAll();
        }
        
        return models.flatMap(model -> makeRepository.findById(model.getMakeId())
                .map(make -> mapToModelResponse(model, make)));
    }

    public Mono<Void> deleteMake(UUID id) {
        return makeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleMake", id)))
                .flatMap(make -> {
                    return modelRepository.existsByMakeIdAndActiveTrue(id)
                            .flatMap(hasActiveModels -> {
                                if (hasActiveModels) {
                                    return Mono.error(new BusinessException("Cannot delete make with active models. Deactivate or delete models first."));
                                }
                                return makeRepository.delete(make);
                            });
                });
    }

    public Mono<VehicleModelResponse> createModel(VehicleModelRequest request) {
        return makeRepository.findById(request.getMakeId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleMake", request.getMakeId())))
                .flatMap(make -> modelRepository.existsByMakeIdAndCode(request.getMakeId(), request.getCode())
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new BusinessException("Model with code " + request.getCode() + " already exists for this make"));
                            }
                            VehicleModel model = VehicleModel.builder()
                                    .id(UUID.randomUUID())
                                    .makeId(make.getId())
                                    .code(request.getCode().toUpperCase())
                                    .name(request.getName())
                                    .yearFrom(request.getYearFrom())
                                    .yearTo(request.getYearTo())
                                    .bodyType(request.getBodyType())
                                    .fuelType(request.getFuelType())
                                    .engineCapacityCc(request.getEngineCapacityCc())
                                    .active(request.isActive())
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build();
                            return modelRepository.save(model).map(m -> mapToModelResponse(m, make));
                        }));
    }

    public Mono<VehicleModelResponse> updateModel(UUID id, VehicleModelRequest request) {
        return modelRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleModel", id)))
                .flatMap(model -> makeRepository.findById(model.getMakeId())
                        .flatMap(make -> {
                            model.setName(request.getName());
                            model.setYearFrom(request.getYearFrom());
                            model.setYearTo(request.getYearTo());
                            model.setBodyType(request.getBodyType());
                            model.setFuelType(request.getFuelType());
                            model.setEngineCapacityCc(request.getEngineCapacityCc());
                            model.setActive(request.isActive());
                            model.setUpdatedAt(LocalDateTime.now());
                            return modelRepository.save(model).map(m -> mapToModelResponse(m, make));
                        }));
    }

    public Mono<VehicleModelResponse> getModel(UUID id) {
        return modelRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleModel", id)))
                .flatMap(model -> makeRepository.findById(model.getMakeId())
                        .map(make -> mapToModelResponse(model, make)));
    }

    public Flux<VehicleModelResponse> getModelsByMakeId(UUID makeId) {
        return makeRepository.findById(makeId)
                .flatMapMany(make -> modelRepository.findByMakeId(makeId)
                        .map(model -> mapToModelResponse(model, make)));
    }

    public Flux<VehicleModelResponse> getModelsByMakeCode(String makeCode) {
        return makeRepository.findByCode(makeCode)
                .flatMapMany(make -> modelRepository.findByMakeCode(makeCode)
                        .map(model -> mapToModelResponse(model, make)));
    }

    public Mono<Void> deleteModel(UUID id) {
        return modelRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("VehicleModel", id)))
                .flatMap(modelRepository::delete);
    }

    private Mono<Void> deactivateModelsForMake(UUID makeId) {
        return modelRepository.findByMakeId(makeId)
                .flatMap(model -> {
                    model.setActive(false);
                    return modelRepository.save(model);
                }).then();
    }

    private VehicleMakeResponse mapToMakeResponse(VehicleMake make) {
        return VehicleMakeResponse.builder()
                .id(make.getId())
                .code(make.getCode())
                .name(make.getName())
                .country(make.getCountry())
                .active(make.isActive())
                .createdAt(make.getCreatedAt())
                .updatedAt(make.getUpdatedAt())
                .build();
    }

    private VehicleModelResponse mapToModelResponse(VehicleModel model, VehicleMake make) {
        return VehicleModelResponse.builder()
                .id(model.getId())
                .makeId(model.getMakeId())
                .makeCode(make.getCode())
                .makeName(make.getName())
                .code(model.getCode())
                .name(model.getName())
                .yearFrom(model.getYearFrom())
                .yearTo(model.getYearTo())
                .bodyType(model.getBodyType())
                .fuelType(model.getFuelType())
                .engineCapacityCc(model.getEngineCapacityCc())
                .active(model.isActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}

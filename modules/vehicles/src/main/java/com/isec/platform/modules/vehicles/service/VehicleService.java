package com.isec.platform.modules.vehicles.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.vehicles.domain.VehicleMake;
import com.isec.platform.modules.vehicles.domain.VehicleModel;
import com.isec.platform.modules.vehicles.dto.VehicleMakeRequest;
import com.isec.platform.modules.vehicles.dto.VehicleMakeResponse;
import com.isec.platform.modules.vehicles.dto.VehicleModelRequest;
import com.isec.platform.modules.vehicles.dto.VehicleModelResponse;
import com.isec.platform.modules.vehicles.exception.DuplicateResourceException;
import com.isec.platform.modules.vehicles.repository.VehicleMakeRepository;
import com.isec.platform.modules.vehicles.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleMakeRepository makeRepository;
    private final VehicleModelRepository modelRepository;

    // --- Vehicle Make ---

    @Transactional
    public VehicleMakeResponse createMake(VehicleMakeRequest request) {
        if (makeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Vehicle make with code " + request.getCode() + " already exists");
        }

        VehicleMake make = VehicleMake.builder()
                .code(request.getCode())
                .name(request.getName())
                .country(request.getCountry())
                .active(request.isActive())
                .build();

        make = makeRepository.save(make);
        return mapToMakeResponse(make);
    }

    @Transactional
    public VehicleMakeResponse updateMake(UUID id, VehicleMakeRequest request) {
        VehicleMake make = makeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMake", id));

        if (!make.getCode().equals(request.getCode()) && makeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Vehicle make with code " + request.getCode() + " already exists");
        }

        make.setCode(request.getCode());
        make.setName(request.getName());
        make.setCountry(request.getCountry());
        
        boolean wasActive = make.isActive();
        make.setActive(request.isActive());

        // Business Rule: Deactivating a Make should optionally deactivate its Models (configurable - here we do it if active changed from true to false)
        if (wasActive && !request.isActive()) {
            deactivateModelsForMake(make.getId());
        }

        make = makeRepository.save(make);
        return mapToMakeResponse(make);
    }

    public VehicleMakeResponse getMake(UUID id) {
        return makeRepository.findById(id)
                .map(this::mapToMakeResponse)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMake", id));
    }

    public Page<VehicleMakeResponse> getAllMakes(Boolean active, Pageable pageable) {
        Specification<VehicleMake> spec = Specification.where(null);
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        return makeRepository.findAll(spec, pageable).map(this::mapToMakeResponse);
    }

    @Transactional
    public void deleteMake(UUID id) {
        VehicleMake make = makeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMake", id));

        // Business Rule: Cannot delete a Make if it has active Models
        if (modelRepository.existsByMakeIdAndActiveTrue(id)) {
            throw new IllegalStateException("Cannot delete Make with active Models");
        }

        make.setActive(false);
        makeRepository.save(make);
    }

    // --- Vehicle Model ---

    @Transactional
    public VehicleModelResponse createModel(VehicleModelRequest request) {
        VehicleMake make = makeRepository.findById(request.getMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMake", request.getMakeId()));

        if (modelRepository.existsByMakeIdAndCode(request.getMakeId(), request.getCode())) {
            throw new DuplicateResourceException("Vehicle model with code " + request.getCode() + " already exists for this make");
        }

        VehicleModel model = VehicleModel.builder()
                .make(make)
                .code(request.getCode())
                .name(request.getName())
                .yearFrom(request.getYearFrom())
                .yearTo(request.getYearTo())
                .bodyType(request.getBodyType())
                .fuelType(request.getFuelType())
                .engineCapacityCc(request.getEngineCapacityCc())
                .active(request.isActive())
                .build();

        model = modelRepository.save(model);
        return mapToModelResponse(model);
    }

    @Transactional
    public VehicleModelResponse updateModel(UUID id, VehicleModelRequest request) {
        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", id));

        if (!model.getMake().getId().equals(request.getMakeId())) {
             VehicleMake make = makeRepository.findById(request.getMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("VehicleMake", request.getMakeId()));
             model.setMake(make);
        }

        if (!model.getCode().equals(request.getCode()) && modelRepository.existsByMakeIdAndCode(request.getMakeId(), request.getCode())) {
            throw new DuplicateResourceException("Vehicle model with code " + request.getCode() + " already exists for this make");
        }

        model.setCode(request.getCode());
        model.setName(request.getName());
        model.setYearFrom(request.getYearFrom());
        model.setYearTo(request.getYearTo());
        model.setBodyType(request.getBodyType());
        model.setFuelType(request.getFuelType());
        model.setEngineCapacityCc(request.getEngineCapacityCc());
        model.setActive(request.isActive());

        model = modelRepository.save(model);
        return mapToModelResponse(model);
    }

    public VehicleModelResponse getModel(UUID id) {
        return modelRepository.findById(id)
                .map(this::mapToModelResponse)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", id));
    }

    public Page<VehicleModelResponse> getAllModels(UUID makeId, String makeCode, Boolean active, Pageable pageable) {
        Specification<VehicleModel> spec = Specification.where(null);
        if (makeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("make").get("id"), makeId));
        }
        if (makeCode != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("make").get("code"), makeCode));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        return modelRepository.findAll(spec, pageable).map(this::mapToModelResponse);
    }

    public List<VehicleModelResponse> getModelsByMakeId(UUID makeId) {
        return modelRepository.findByMakeId(makeId).stream()
                .map(this::mapToModelResponse)
                .collect(Collectors.toList());
    }

    public List<VehicleModelResponse> getModelsByMakeCode(String makeCode) {
        return modelRepository.findByMakeCode(makeCode).stream()
                .map(this::mapToModelResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteModel(UUID id) {
        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", id));
        model.setActive(false);
        modelRepository.save(model);
    }

    private void deactivateModelsForMake(UUID makeId) {
        List<VehicleModel> models = modelRepository.findByMakeId(makeId);
        models.forEach(m -> m.setActive(false));
        modelRepository.saveAll(models);
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

    private VehicleModelResponse mapToModelResponse(VehicleModel model) {
        return VehicleModelResponse.builder()
                .id(model.getId())
                .makeId(model.getMake().getId())
                .makeCode(model.getMake().getCode())
                .makeName(model.getMake().getName())
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

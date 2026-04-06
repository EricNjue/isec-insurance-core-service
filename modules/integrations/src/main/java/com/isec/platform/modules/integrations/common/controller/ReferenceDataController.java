package com.isec.platform.modules.integrations.common.controller;

import com.isec.platform.modules.integrations.common.dto.referencedata.MasterReferenceDataResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.SingleCategoryReferenceDataResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.DependentReferenceDataResponse;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import com.isec.platform.modules.integrations.common.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/integrations/{companyCode}/reference-data")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping
    public ResponseEntity<MasterReferenceDataResponse> getMasterReferenceData(
            @PathVariable String companyCode,
            @RequestParam String productCode) {
        return ResponseEntity.ok(referenceDataService.getMasterReferenceData(companyCode, productCode));
    }

    @GetMapping("/{categoryKey}")
    public ResponseEntity<SingleCategoryReferenceDataResponse> getSingleCategoryReferenceData(
            @PathVariable String companyCode,
            @PathVariable ReferenceCategory categoryKey,
            @RequestParam String productCode) {
        return ResponseEntity.ok(referenceDataService.getSingleCategoryReferenceData(companyCode, productCode, categoryKey));
    }

    @GetMapping("/dependent")
    public ResponseEntity<DependentReferenceDataResponse> getDependentReferenceData(
            @PathVariable String companyCode,
            @RequestParam String productCode,
            @RequestParam ReferenceCategory parentCategoryKey,
            @RequestParam String parentValue,
            @RequestParam ReferenceCategory childCategoryKey) {
        return ResponseEntity.ok(referenceDataService.getDependentReferenceData(
                companyCode, productCode, parentCategoryKey, parentValue, childCategoryKey));
    }
}

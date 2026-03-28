package com.isec.platform.modules.integrations.sanlam.adapter;

import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDoubleInsuranceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamIntegrationAdapter implements InsuranceIntegrationAdapter {

    private final SanlamClient sanlamClient;

    @Override
    public String getCompanyCode() {
        return "SANLAM";
    }

    @Override
    public DoubleInsuranceCheckResponse checkDoubleInsurance(DoubleInsuranceCheckRequest request) {
        log.info("Starting double insurance check for Sanlam. Registration: {}", request.getRegistrationNumber());
        try {
            SanlamDoubleInsuranceResponse response = sanlamClient.checkDoubleInsurance(
                    request.getRegistrationNumber(),
                    request.getChassisNumber()
            );

            log.info("Sanlam API response received: status={}, message={}", response.getStatus(), response.getMessage());

            boolean hasDuplicate = "double".equalsIgnoreCase(response.getStatus());
            
            DoubleInsuranceCheckResponse canonicalResponse = DoubleInsuranceCheckResponse.builder()
                    .hasDuplicate(hasDuplicate)
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

            if (hasDuplicate && response.getEvidence() != null) {
                log.info("Duplicate cover found for vehicle {}. Insurer: {}, Expiry: {}", 
                        request.getRegistrationNumber(), response.getEvidence().getInsurer(), response.getEvidence().getCoverEndDate());
                canonicalResponse.setDetails(DoubleInsuranceCheckResponse.DuplicateDetails.builder()
                        .insurer(response.getEvidence().getInsurer())
                        .policyNumber(response.getEvidence().getPolicyNumber())
                        .certificateNumber(response.getEvidence().getCertificateNumber())
                        .expiryDate(response.getEvidence().getCoverEndDate())
                        .status(response.getEvidence().getCertificateStatus())
                        .build());
            } else {
                log.info("No active duplicate cover found for vehicle {} via Sanlam check", request.getRegistrationNumber());
            }

            return canonicalResponse;
        } catch (Exception e) {
            log.error("Error during Sanlam double insurance check: {}", e.getMessage(), e);
            throw e;
        }
    }
}

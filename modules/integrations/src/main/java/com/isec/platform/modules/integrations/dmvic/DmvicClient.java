package com.isec.platform.modules.integrations.dmvic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Slf4j
public class DmvicClient {

    public String issueCertificate(String regNumber, String policyNumber) {
        log.info("Calling DMVIC API for vehicle: {} and policy: {}", regNumber, policyNumber);
        // Mocking DMVIC API call
        return "DMVIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

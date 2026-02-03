package com.isec.platform.modules.integrations.dmvic;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class DmvicClient {

    public String issueCertificate(String regNumber, String policyNumber) {
        // Mocking DMVIC API call
        return "DMVIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

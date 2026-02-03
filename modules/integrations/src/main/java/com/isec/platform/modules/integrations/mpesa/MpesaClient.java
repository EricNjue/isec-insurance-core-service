package com.isec.platform.modules.integrations.mpesa;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class MpesaClient {
    
    public MpesaResponse initiateStkPush(String phoneNumber, BigDecimal amount, String accountRef) {
        // In a real implementation, this would use RestTemplate/WebClient 
        // to call Safaricom API with OAuth2 token.
        return new MpesaResponse("0", "Success", "CheckoutID_" + System.currentTimeMillis());
    }

    public record MpesaResponse(String responseCode, String responseDescription, String checkoutRequestId) {}
}

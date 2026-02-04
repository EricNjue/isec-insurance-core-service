package com.isec.platform.modules.integrations.mpesa;

import com.isec.platform.modules.integrations.mpesa.dto.MpesaDtos;
import com.isec.platform.modules.integrations.mpesa.service.MpesaStkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaClient {

    private final MpesaStkService mpesaStkService;
    
    public MpesaResponse initiateStkPush(String phoneNumber, BigDecimal amount, String accountRef) {
        log.info("MpesaClient: initiating STK push for {}, amount {}", accountRef, amount);
        MpesaDtos.StkPushResponse response = mpesaStkService.initiateStkPush(
                amount.intValue(),
                phoneNumber,
                accountRef,
                "Lipa Na MPESA"
        ).block();

        if (response == null) {
            log.error("MpesaClient: Received null response from STK service");
            return new MpesaResponse("1", "Failed to get response from M-PESA", null);
        }

        log.info("MpesaClient: STK push initiated successfully. ResponseCode: {}, CheckoutRequestID: {}", 
                response.getResponseCode(), response.getCheckoutRequestID());
        return new MpesaResponse(response.getResponseCode(), response.getResponseDescription(), response.getCheckoutRequestID());
    }

    public record MpesaResponse(String responseCode, String responseDescription, String checkoutRequestId) {}
}

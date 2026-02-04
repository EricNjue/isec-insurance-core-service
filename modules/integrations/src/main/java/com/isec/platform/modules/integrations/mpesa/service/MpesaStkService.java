package com.isec.platform.modules.integrations.mpesa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.integrations.mpesa.config.MpesaConfig;
import com.isec.platform.modules.integrations.mpesa.domain.MpesaRequestLog;
import com.isec.platform.modules.integrations.mpesa.dto.MpesaDtos;
import com.isec.platform.modules.integrations.mpesa.repository.MpesaRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpesaStkService {

    private final MpesaConfig mpesaConfig;
    private final MpesaOAuthService oAuthService;
    private final WebClient.Builder webClientBuilder;
    private final MpesaRequestLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public Mono<MpesaDtos.StkPushResponse> initiateStkPush(Integer amount, String phoneNumber, String accountReference, String transactionDesc) {
        log.info("Preparing STK Push request for phone: {}, amount: {}, reference: {}", 
                phoneNumber != null && phoneNumber.length() >= 4 ? "***" + phoneNumber.substring(phoneNumber.length() - 4) : phoneNumber, 
                amount, accountReference);
        
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String password = generatePassword(mpesaConfig.getShortCode(), mpesaConfig.getPasskey(), timestamp);

        MpesaDtos.StkPushRequest request = MpesaDtos.StkPushRequest.builder()
                .businessShortCode(mpesaConfig.getShortCode())
                .password(password)
                .timestamp(timestamp)
                .transactionType("CustomerPayBillOnline")
                .amount(amount)
                .partyA(phoneNumber)
                .partyB(mpesaConfig.getShortCode())
                .phoneNumber(phoneNumber)
                .callBackUrl(mpesaConfig.getCallbackUrl())
                .accountReference(accountReference)
                .transactionDesc(transactionDesc)
                .build();

        String accessToken = oAuthService.getAccessToken();

        return webClientBuilder.build()
                .post()
                .uri(mpesaConfig.getStkPushUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MpesaDtos.StkPushResponse.class)
                .doOnNext(response -> {
                    log.info("STK Push Response: {}", response);
                    saveLog("STK_PUSH", request, response);
                })
                .doOnError(error -> {
                    log.error("STK Push failed: {}", error.getMessage());
                    saveLog("STK_PUSH", request, error.getMessage());
                });
    }

    public Mono<MpesaDtos.StkQueryResponse> queryStkPush(String checkoutRequestId) {
        log.info("Querying STK Push status for checkoutRequestId: {}", checkoutRequestId);
        
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String password = generatePassword(mpesaConfig.getShortCode(), mpesaConfig.getPasskey(), timestamp);

        MpesaDtos.StkQueryRequest request = MpesaDtos.StkQueryRequest.builder()
                .businessShortCode(mpesaConfig.getShortCode())
                .password(password)
                .timestamp(timestamp)
                .checkoutRequestID(checkoutRequestId)
                .build();

        String accessToken = oAuthService.getAccessToken();

        return webClientBuilder.build()
                .post()
                .uri(mpesaConfig.getStkQueryUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MpesaDtos.StkQueryResponse.class)
                .doOnNext(response -> {
                    log.info("STK Query Response: {}", response);
                    saveLog("STK_QUERY", request, response);
                })
                .doOnError(error -> {
                    log.error("STK Query failed: {}", error.getMessage());
                    saveLog("STK_QUERY", request, error.getMessage());
                });
    }

    private void saveLog(String type, Object request, Object response) {
        try {
            MpesaRequestLog.MpesaRequestLogBuilder logBuilder = MpesaRequestLog.builder()
                    .requestType(type)
                    .requestPayload(objectMapper.writeValueAsString(request));

            if (response instanceof String) {
                logBuilder.responsePayload((String) response);
            } else {
                logBuilder.responsePayload(objectMapper.writeValueAsString(response));
                
                if (response instanceof MpesaDtos.StkPushResponse stkResponse) {
                    logBuilder.checkoutRequestId(stkResponse.getCheckoutRequestID())
                            .merchantRequestId(stkResponse.getMerchantRequestID())
                            .responseCode(stkResponse.getResponseCode());
                } else if (response instanceof MpesaDtos.StkQueryResponse queryResponse) {
                    logBuilder.checkoutRequestId(queryResponse.getCheckoutRequestID())
                            .merchantRequestId(queryResponse.getMerchantRequestID())
                            .responseCode(queryResponse.getResponseCode());
                }
            }

            logRepository.save(logBuilder.build());
        } catch (Exception e) {
            log.error("Failed to save M-PESA request log", e);
        }
    }

    private String generatePassword(String shortCode, String passkey, String timestamp) {
        String str = shortCode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }
}

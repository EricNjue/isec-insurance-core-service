package com.isec.platform.modules.certificates.adapters.provider.jubilee;

import com.isec.platform.modules.certificates.adapters.CertificateProviderAdapter;
import com.isec.platform.modules.certificates.adapters.provider.jubilee.dto.JubileeCertificateRequest;
import com.isec.platform.modules.certificates.adapters.provider.jubilee.dto.JubileeCertificateResponse;
import com.isec.platform.modules.certificates.adapters.provider.jubilee.dto.JubileeStatusResponse;
import com.isec.platform.modules.certificates.adapters.provider.jubilee.mapper.JubileeCertificateMapper;
import com.isec.platform.modules.certificates.config.ProviderConfiguration;
import com.isec.platform.modules.certificates.config.ProviderConfigurationService;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.exception.ProviderException;
import com.isec.platform.modules.certificates.exception.ProviderTimeoutException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JubileeCertificateAdapter implements CertificateProviderAdapter {

    @Qualifier("jubileeWebClient")
    private final WebClient jubileeWebClient;
    private final JubileeCertificateMapper mapper;
    private final ProviderConfigurationService configurationService;

    @Override
    public ProviderType providerType() {
        return ProviderType.JUBILEE;
    }

    @Override
    @CircuitBreaker(name = "jubilee")
    @Retry(name = "jubilee")
    public CertificateResponse issueCertificate(CertificateRequest request) {
        ProviderConfiguration configuration = configurationService.getConfiguration(ProviderType.JUBILEE);
        JubileeCertificateRequest providerRequest = mapper.toProviderRequest(request);
        try {
            JubileeCertificateResponse response = jubileeWebClient.post()
                    .uri("/certificates")
                    .bodyValue(providerRequest)
                    .retrieve()
                    .bodyToMono(JubileeCertificateResponse.class)
                    .timeout(Duration.ofMillis(configuration.timeoutMs()))
                    .block();

            if (response == null) {
                throw new ProviderException("Jubilee response was empty");
            }
            return mapper.toCanonicalResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("Jubilee API error: status={} body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new ProviderException("Jubilee API error", ex);
        } catch (Exception ex) {
            if (ex.getCause() instanceof TimeoutException) {
                throw new ProviderTimeoutException("Jubilee API timed out", ex);
            }
            throw new ProviderException("Jubilee API call failed", ex);
        }
    }

    @Override
    @CircuitBreaker(name = "jubilee")
    @Retry(name = "jubilee")
    public CertificateResponse checkStatus(String externalReference) {
        ProviderConfiguration configuration = configurationService.getConfiguration(ProviderType.JUBILEE);
        try {
            JubileeStatusResponse response = jubileeWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/certificates/status")
                            .queryParam("reference", externalReference)
                            .build())
                    .retrieve()
                    .bodyToMono(JubileeStatusResponse.class)
                    .timeout(Duration.ofMillis(configuration.timeoutMs()))
                    .block();

            if (response == null) {
                throw new ProviderException("Jubilee status response was empty");
            }
            return mapper.toCanonicalResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("Jubilee status API error: status={} body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new ProviderException("Jubilee status API error", ex);
        } catch (Exception ex) {
            if (ex.getCause() instanceof TimeoutException) {
                throw new ProviderTimeoutException("Jubilee status API timed out", ex);
            }
            throw new ProviderException("Jubilee status API call failed", ex);
        }
    }
}

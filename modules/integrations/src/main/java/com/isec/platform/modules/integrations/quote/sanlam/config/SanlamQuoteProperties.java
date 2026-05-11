package com.isec.platform.modules.integrations.quote.sanlam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "integrations.quote.sanlam")
public class SanlamQuoteProperties {
    private String baseUrl;
    private String createDraftQuotePath = "/quotes/create_draft_quote";
    private String getDraftQuotePath = "/quotes/draft_quote/{draftQuoteSysId}";
    private String updateDraftQuotePath = "/quotes/draft_quote/{draftQuoteSysId}";
    private String sendDocumentsEmailPath = "/quotes/send-insurance-documents-email";
    private Duration timeout = Duration.ofSeconds(5);
    private PaymentProperties payment = new PaymentProperties();
    private RetryProperties retry = new RetryProperties();

    @Data
    public static class PaymentProperties {
        private int defaultInstallmentCount = 2;
        private int maxInstallments = 3;
    }

    @Data
    public static class RetryProperties {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(500);
    }
}

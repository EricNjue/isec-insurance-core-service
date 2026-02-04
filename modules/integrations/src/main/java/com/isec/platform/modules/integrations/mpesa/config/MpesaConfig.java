package com.isec.platform.modules.integrations.mpesa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mpesa")
public class MpesaConfig {
    private String consumerKey;
    private String consumerSecret;
    private String shortCode;
    private String passkey;
    private String callbackUrl;
    private String oauthUrl;
    private String stkPushUrl;
    private String stkQueryUrl;
}

package com.isec.platform.modules.certificates.adapters.provider.jubilee.config;

import com.isec.platform.modules.certificates.config.ProviderConfiguration;
import com.isec.platform.modules.certificates.config.ProviderConfigurationService;
import com.isec.platform.modules.certificates.config.ProviderWebClientFactory;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class JubileeWebClientConfig {

    @Bean
    @Lazy
    public WebClient jubileeWebClient(ProviderConfigurationService configurationService,
                                      ProviderWebClientFactory webClientFactory) {
        ProviderConfiguration configuration = configurationService.getConfiguration(ProviderType.JUBILEE);
        return webClientFactory.buildClient(configuration);
    }
}

package com.isec.platform.modules.applications.service.motor;

import com.isec.platform.modules.applications.domain.motor.PartnerPaymentAccount;
import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.applications.repository.motor.PartnerPaymentAccountRepository;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerPaymentAccountService {

    private final PartnerPaymentAccountRepository repository;

    @Value("${app.payment.environment:UAT}")
    private String environment;

    public Mono<PartnerPaymentAccount> getDefaultActiveAccount(
            PartnerType partner, 
            String provider, 
            PaymentMethod method
    ) {
        log.info("Resolving default active account for partner: {}, provider: {}, method: {}, environment: {}", 
                partner, provider, method, environment);
        return repository.findFirstByPartnerCodeAndPaymentProviderAndPaymentMethodAndEnvironmentAndIsActiveTrueAndIsDefaultTrue(
                partner, provider, method, environment
        ).switchIfEmpty(Mono.error(new RuntimeException(
                String.format("No active default payment account found for partner %s, provider %s, method %s in %s environment", 
                        partner, provider, method, environment))));
    }

    public Flux<PartnerPaymentAccount> findAll() {
        return repository.findAll();
    }

    public Mono<PartnerPaymentAccount> findById(UUID id) {
        return repository.findById(id);
    }

    public Mono<PartnerPaymentAccount> save(PartnerPaymentAccount account) {
        return repository.save(account);
    }

    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id);
    }
}

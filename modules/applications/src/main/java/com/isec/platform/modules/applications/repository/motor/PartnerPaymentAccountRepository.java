package com.isec.platform.modules.applications.repository.motor;

import com.isec.platform.modules.applications.domain.motor.PartnerPaymentAccount;
import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PartnerPaymentAccountRepository extends ReactiveCrudRepository<PartnerPaymentAccount, UUID> {
    
    Mono<PartnerPaymentAccount> findFirstByPartnerCodeAndPaymentProviderAndPaymentMethodAndEnvironmentAndIsActiveTrueAndIsDefaultTrue(
            PartnerType partnerCode, 
            String paymentProvider, 
            PaymentMethod paymentMethod, 
            String environment
    );
}

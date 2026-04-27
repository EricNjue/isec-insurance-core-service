package com.isec.platform.modules.certificates.service;

import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.NotificationChannel;
import com.isec.platform.messaging.events.NotificationSendEvent;
import com.isec.platform.modules.certificates.domain.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateResendService {

    private final CertificateRetrievalService retrievalService;
    private final RabbitTemplate rabbitTemplate;

    public Mono<Void> resendCertificate(String certificateNumber, String overrideEmail) {
        return retrievalService.getCertificateMetadata(certificateNumber)
                .flatMap(cert -> {
                    String recipient = (overrideEmail != null && !overrideEmail.isBlank()) 
                            ? overrideEmail 
                            : cert.getCustomerEmail();
                    
                    if (recipient == null || recipient.isBlank()) {
                        return Mono.error(new IllegalArgumentException("No recipient email available for resend"));
                    }

                    log.info("Requesting resend of certificate {} to {}", certificateNumber, recipient);

                    return retrievalService.generateDownloadUrl(certificateNumber)
                            .flatMap(downloadUrl -> Mono.fromRunnable(() -> {
                                NotificationSendEvent event = NotificationSendEvent.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .recipient(recipient)
                                        .channel(NotificationChannel.EMAIL)
                                        .subject("Your Insurance Certificate - " + cert.getCertificateNumber())
                                        .content("Please find your insurance certificate for policy " + cert.getPolicyNumber() + " at the following link: " + downloadUrl)
                                        .correlationId(UUID.randomUUID().toString())
                                        .build();

                                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.NOTIFICATION_SEND_RK, event);
                                log.info("Certificate resend event published for cert: {}", certificateNumber);
                            }).subscribeOn(Schedulers.boundedElastic()));
                }).then();
    }
}

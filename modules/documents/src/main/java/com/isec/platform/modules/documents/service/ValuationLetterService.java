package com.isec.platform.modules.documents.service;

import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.ValuationLetterRequestedEvent;
import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.AuthorizedValuerRepository;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValuationLetterService {

    private final ValuationLetterRepository letterRepository;
    private final AuthorizedValuerRepository valuerRepository;
    private final PolicyRepository policyRepository;
    private final RabbitTemplate rabbitTemplate;
    private final S3Service s3Service;
    private final PdfGenerationService pdfGenerationService;
    private final PdfSecurityService pdfSecurityService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public Mono<ValuationLetter> getLatestLetter(Long policyId) {
        return letterRepository.findFirstByPolicyIdOrderByGeneratedAtDesc(policyId)
                .doOnNext(l -> log.info("Found latest valuation letter for policyId={} generatedAt={}", policyId, l.getGeneratedAt()));
    }

    public Mono<ValuationLetter> generateIfNotExists(Long policyId, String insuredName, String registrationNumber, boolean force) {
        log.info("Generate valuation letter request received. policyId={}, insuredName={}, registrationNumber={}, force={}"
                , policyId, insuredName, registrationNumber, force);
        
        Mono<ValuationLetter> latestMono = force ? Mono.empty() : getLatestLetter(policyId);

        return latestMono
                .flatMap(existing -> {
                    log.info("Returning latest valuation letter id={} for policyId={} (idempotent)", existing.getId(), policyId);
                    return Mono.just(existing);
                })
                .switchIfEmpty(Mono.defer(() -> processGeneration(policyId, insuredName, registrationNumber)));
    }

    private Mono<ValuationLetter> processGeneration(Long policyId, String insuredName, String registrationNumber) {
        log.debug("Starting processGeneration for policyId={}", policyId);
        return policyRepository.findById(policyId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Policy not found: " + policyId)))
                .flatMap(policy -> {
                    log.debug("Loaded policy. policyNumber={}, startDate={}, expiryDate={}", policy.getPolicyNumber(), policy.getStartDate(), policy.getExpiryDate());

                    ValuationLetter letter = ValuationLetter.builder()
                            .policyId(policyId)
                            .insuredName(insuredName)
                            .policyNumber(policy.getPolicyNumber())
                            .vehicleRegistrationNumber(registrationNumber)
                            .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                            .documentUuid(UUID.randomUUID())
                            .documentType("VALUATION_LETTER")
                            .generatedBy("SYSTEM")
                            .generatedAt(LocalDateTime.now())
                            .build();
                    
                    return letterRepository.save(letter)
                            .flatMap(savedLetter -> valuerRepository.findByActiveTrue().collectList()
                                    .flatMap(valuers -> Mono.fromCallable(() -> {
                                        log.debug("Generating PDF with {} active valuers", valuers.size());
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("insuredName", insuredName);
                                        data.put("policyNumber", policy.getPolicyNumber());
                                        data.put("registrationNumber", registrationNumber);

                                        byte[] pdf = pdfGenerationService.generateValuationLetter(data, valuers, savedLetter);
                                        String hash = pdfSecurityService.calculateHash(pdf);
                                        savedLetter.setDocumentHash(hash);
                                        
                                        String key = "valuation-letters/" + policyId + "/" + savedLetter.getId() + ".pdf";
                                        
                                        savedLetter.setPdfS3Bucket(bucketName);
                                        savedLetter.setPdfS3Key(key);
                                        
                                        policy.setValuationLetterS3Key(key);
                                        
                                        return s3Service.uploadBytesAsync(bucketName, key, pdf, "application/pdf")
                                                .thenReturn(savedLetter);
                                    })
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(savedMono -> savedMono)
                                    .flatMap(saved -> Mono.zip(letterRepository.save(saved), policyRepository.save(policy))
                                            .map(tuple -> tuple.getT1()))));
                });
    }

    public String generateDownloadUrl(ValuationLetter letter) {
        String url = s3Service.generatePresignedGetUrl(letter.getPdfS3Key());
        log.debug("Generated presigned download URL for letter id={}", letter.getId());
        return url;
    }

    public Mono<Void> publishGenerationRequest(Long policyId, String policyNumber, String registrationNumber, String recipientEmail) {
        return Mono.fromRunnable(() -> {
            ValuationLetterRequestedEvent event = ValuationLetterRequestedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .policyId(policyId)
                    .policyNumber(policyNumber)
                    .registrationNumber(registrationNumber)
                    .recipientEmail(recipientEmail)
                    .correlationId(UUID.randomUUID().toString())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.VALUATION_LETTER_REQUESTED_RK, event);
            log.info("Published ValuationLetterRequestedEvent eventId={} policyId={}", event.getEventId(), policyId);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}

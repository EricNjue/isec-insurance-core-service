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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Transactional(readOnly = true)
    public Optional<ValuationLetter> getLatestLetter(Long policyId) {
        Optional<ValuationLetter> found = letterRepository.findFirstByPolicyIdOrderByGeneratedAtDesc(policyId);
        found.ifPresent(l -> log.info("Found latest valuation letter for policyId={} generatedAt={}", policyId, l.getGeneratedAt()));
        return found;
    }

    @Transactional
    public ValuationLetter generateIfNotExists(Long policyId, String insuredName, String registrationNumber, boolean force) {
        log.info("Generate valuation letter request received. policyId={}, insuredName={}, registrationNumber={}, force={}"
                , policyId, insuredName, registrationNumber, force);
        if (!force) {
            Optional<ValuationLetter> existing = getLatestLetter(policyId);
            if (existing.isPresent()) {
                log.info("Returning latest valuation letter id={} for policyId={} (idempotent)", existing.get().getId(), policyId);
                return existing.get();
            }
        }

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        log.debug("Loaded policy. policyNumber={}, startDate={}, expiryDate={}", policy.getPolicyNumber(), policy.getStartDate(), policy.getExpiryDate());

        // Persist shell record first to obtain ID for S3 key
        ValuationLetter letter = ValuationLetter.builder()
                .policyId(policyId)
                .insuredName(insuredName)
                .policyNumber(policy.getPolicyNumber())
                .vehicleRegistrationNumber(registrationNumber)
                .status(ValuationLetter.ValuationLetterStatus.GENERATED)
                .generatedBy("SYSTEM")
                .generatedAt(LocalDateTime.now())
                .build();
        letter = letterRepository.save(letter);
        log.info("Created valuation letter shell record id={} for policyId={}", letter.getId(), policyId);

        // Render PDF
        Map<String, Object> data = new HashMap<>();
        data.put("insuredName", insuredName);
        data.put("policyNumber", policy.getPolicyNumber());
        data.put("registrationNumber", registrationNumber);

        java.util.List<AuthorizedValuer> valuers = valuerRepository.findByActiveTrue();
        log.debug("Generating PDF with {} active valuers", valuers.size());
        byte[] pdf = pdfGenerationService.generateValuationLetter(data, valuers);
        log.info("PDF generated for valuation letter id={} (size={} bytes)", letter.getId(), pdf != null ? pdf.length : 0);

        // Upload to S3 with structured key
        String key = "valuation-letters/" + policyId + "/" + letter.getId() + ".pdf";
        s3Service.uploadBytes(bucketName, key, pdf, "application/pdf");
        log.info("Valuation letter PDF uploaded to S3. bucket={}, key={}", bucketName, key);

        letter.setPdfS3Bucket(bucketName);
        letter.setPdfS3Key(key);
        ValuationLetter saved = letterRepository.save(letter);
        log.info("Valuation letter metadata persisted. id={} status={}", saved.getId(), saved.getStatus());

        // Update Policy with latest S3 key
        policy.setValuationLetterS3Key(key);
        policyRepository.save(policy);
        log.info("Policy {} updated with latest valuation letter S3 key: {}", policy.getPolicyNumber(), key);

        return saved;
    }

    public String generateDownloadUrl(ValuationLetter letter) {
        String url = s3Service.generatePresignedGetUrl(letter.getPdfS3Key());
        log.debug("Generated presigned download URL for letter id={}", letter.getId());
        return url;
    }

    public void publishGenerationRequest(Long policyId, String policyNumber, String registrationNumber, String recipientEmail) {
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
    }
}

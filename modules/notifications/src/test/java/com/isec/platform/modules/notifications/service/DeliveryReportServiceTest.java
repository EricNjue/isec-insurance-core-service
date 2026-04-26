package com.isec.platform.modules.notifications.service;

import com.isec.platform.modules.notifications.model.SmsDeliveryReport;
import com.isec.platform.modules.notifications.model.SmsRecipientResult;
import com.isec.platform.modules.notifications.repository.SmsDeliveryReportRepository;
import com.isec.platform.modules.notifications.repository.SmsRecipientResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryReportServiceTest {

    @Mock
    private SmsDeliveryReportRepository deliveryReportRepository;

    @Mock
    private SmsRecipientResultRepository recipientResultRepository;

    @InjectMocks
    private DeliveryReportService deliveryReportService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void upsertsByMessageId_andUpdatesRecipient() {
        Map<String, String> form = new HashMap<>();
        form.put("id", "ATX-1");
        form.put("phoneNumber", "+254700000000");
        form.put("status", "Failed");
        form.put("failureReason", "DeliveryFailure");
        form.put("retryCount", "1");
        form.put("networkCode", "63902");

        SmsRecipientResult recipient = new SmsRecipientResult();
        when(deliveryReportRepository.findByMessageId("ATX-1")).thenReturn(Mono.empty());
        when(recipientResultRepository.findByMessageId("ATX-1")).thenReturn(Mono.just(recipient));
        when(deliveryReportRepository.save(any(SmsDeliveryReport.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(recipientResultRepository.save(any(SmsRecipientResult.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(deliveryReportService.handleFormPayload(form))
                .verifyComplete();

        // verify upsert save
        verify(deliveryReportRepository, times(1)).save(any(SmsDeliveryReport.class));
        // verify recipient update
        assertThat(recipient.getDeliveryStatus()).isEqualTo("Failed");
        assertThat(recipient.getDeliveryFailureReason()).isEqualTo("DeliveryFailure");
        assertThat(recipient.getDeliveryReportedAt()).isNotNull();

        // second call (idempotent update)
        form.put("status", "Success");
        SmsDeliveryReport existing = new SmsDeliveryReport();
        when(deliveryReportRepository.findByMessageId("ATX-1")).thenReturn(Mono.just(existing));

        StepVerifier.create(deliveryReportService.handleFormPayload(form))
                .verifyComplete();
        
        verify(deliveryReportRepository, times(2)).save(any(SmsDeliveryReport.class));
        assertThat(recipient.getDeliveryStatus()).isEqualTo("Success");
    }

    @Test
    void missingRequiredFields_logsAndReturns() {
        Map<String, String> form = new HashMap<>();
        form.put("id", "");
        form.put("phoneNumber", "+254700000000");
        form.put("status", "Success");

        StepVerifier.create(deliveryReportService.handleFormPayload(form))
                .verifyComplete();
        
        verifyNoInteractions(deliveryReportRepository);
    }
}

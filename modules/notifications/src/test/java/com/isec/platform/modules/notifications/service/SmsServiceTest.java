package com.isec.platform.modules.notifications.service;

import com.isec.platform.modules.notifications.dto.SmsSendResult;
import com.isec.platform.modules.notifications.messaging.AfricasTalkingSmsClient;
import com.isec.platform.modules.notifications.model.SmsMessage;
import com.isec.platform.modules.notifications.repository.SmsMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private AfricasTalkingSmsClient smsClient;

    @Mock
    private SmsMessageRepository smsMessageRepository;

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void sendSms_Success_PersistsResult() {
        // Arrange
        String to = "+254719531872";
        String message = "Hello";
        
        SmsSendResult.RecipientResult recipientResult = SmsSendResult.RecipientResult.builder()
                .number(to)
                .status("Success")
                .statusCode(101)
                .messageId("ATXid_123")
                .cost("KES 0.8")
                .build();
        
        SmsSendResult sendResult = SmsSendResult.builder()
                .summaryMessage("Sent to 1/1")
                .recipients(List.of(recipientResult))
                .overallSuccess(true)
                .build();

        when(smsClient.sendSms(to, message)).thenReturn(Mono.just(sendResult));

        // Act
        smsService.sendSms(to, message);

        // Assert
        verify(smsClient).sendSms(to, message);
        
        ArgumentCaptor<SmsMessage> smsMessageCaptor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsMessageRepository).save(smsMessageCaptor.capture());
        
        SmsMessage savedMessage = smsMessageCaptor.getValue();
        assertEquals(to, savedMessage.getTo());
        assertEquals(message, savedMessage.getMessage());
        assertEquals("AfricasTalking", savedMessage.getProvider());
        assertEquals("ATXid_123", savedMessage.getProviderRequestId());
        assertEquals("Sent to 1/1", savedMessage.getStatusSummary());
        assertEquals(1, savedMessage.getRecipientResults().size());
        assertEquals("ATXid_123", savedMessage.getRecipientResults().get(0).getMessageId());
    }

    @Test
    void sendSms_Success_PersistsResult_WithAutoAppendPlus() {
        // Arrange
        String to = "254719531872";
        String expectedTo = "+254719531872";
        String message = "Hello";
        
        SmsSendResult.RecipientResult recipientResult = SmsSendResult.RecipientResult.builder()
                .number(expectedTo)
                .status("Success")
                .statusCode(101)
                .messageId("ATXid_123")
                .cost("KES 0.8")
                .build();
        
        SmsSendResult sendResult = SmsSendResult.builder()
                .summaryMessage("Sent to 1/1")
                .recipients(List.of(recipientResult))
                .overallSuccess(true)
                .build();

        when(smsClient.sendSms(expectedTo, message)).thenReturn(Mono.just(sendResult));

        // Act
        smsService.sendSms(to, message);

        // Assert
        verify(smsClient).sendSms(expectedTo, message);
        
        ArgumentCaptor<SmsMessage> smsMessageCaptor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsMessageRepository).save(smsMessageCaptor.capture());
        
        SmsMessage savedMessage = smsMessageCaptor.getValue();
        assertEquals(expectedTo, savedMessage.getTo());
    }

    @Test
    void sendSms_InvalidPhone_TooShort_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> smsService.sendSms("+2547", "Hello"));
        verifyNoInteractions(smsClient, smsMessageRepository);
    }

    @Test
    void sendSms_InvalidPhone_NonDigits_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> smsService.sendSms("+2547abc123", "Hello"));
        verifyNoInteractions(smsClient, smsMessageRepository);
    }

    @Test
    void sendSms_EmptyMessage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> smsService.sendSms("+254719531872", ""));
        verifyNoInteractions(smsClient, smsMessageRepository);
    }
}

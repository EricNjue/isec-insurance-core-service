package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.config.EmailPollingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailIngestionSchedulerTest {

    private EmailIngestionScheduler scheduler;

    @Mock
    private CertificateIngestionOrchestrator orchestrator;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailPollingProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        properties = new EmailPollingProperties();
        properties.setIdleIntervalSeconds(60);
        properties.setActiveIntervalSeconds(20);
        properties.setActiveModeDurationSeconds(300);
        properties.setLockEnabled(true);
        properties.setLockTtlSeconds(300);
        properties.setJitterPercentage(0); // For predictable testing

        scheduler = new EmailIngestionScheduler(orchestrator, properties, taskScheduler, redisTemplate);
        
        // Set values that are injected via @Value
        ReflectionTestUtils.setField(scheduler, "host", "localhost");
        ReflectionTestUtils.setField(scheduler, "username", "test");
        ReflectionTestUtils.setField(scheduler, "password", "pass");
    }

    @Test
    void scheduleNext_ShouldScheduleWithTaskScheduler() {
        scheduler.scheduleNext();
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void pollEmails_ShouldRespectLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        
        scheduler.pollEmails();
        
        // Should return early and not connect to IMAP
        // Since connectivity logic is internal, we verify no further redis interaction after failed lock
        verify(valueOperations, never()).get(anyString());
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class)); // Still schedules next
    }

    @Test
    void pollEmails_ShouldAcquireAndReleaseLock() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        String lockValue = (String) ReflectionTestUtils.getField(scheduler, "lockValue");
        when(valueOperations.get(anyString())).thenReturn(lockValue);
        
        // This will fail because it tries to connect to IMAP, but we want to see it reached there
        try {
            scheduler.pollEmails();
        } catch (Exception ignored) {
            // Expected IMAP failure
        }
        
        verify(valueOperations).setIfAbsent(eq("lock:email-ingestion"), eq(lockValue), any(Duration.class));
        verify(redisTemplate).delete(eq("lock:email-ingestion"));
    }
}

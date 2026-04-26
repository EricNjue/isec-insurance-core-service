package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.config.EmailPollingProperties;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailIngestionScheduler {

    private final CertificateIngestionOrchestrator orchestrator;
    private final EmailPollingProperties pollingProperties;
    private final TaskScheduler taskScheduler;
    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();
    private final String lockValue = UUID.randomUUID().toString();
    private static final String LOCK_KEY = "lock:email-ingestion";

    @Value("${ingestion.email.host}")
    private String host;

    @Value("${ingestion.email.port}")
    private int port;

    @Value("${ingestion.email.username}")
    private String username;

    @Value("${ingestion.email.password}")
    private String password;

    @Value("${ingestion.email.protocol:imaps}")
    private String protocol;

    @Value("${ingestion.email.folder:INBOX}")
    private String folderName;

    private Instant lastActivityTime = Instant.MIN;
    private boolean isActiveMode = false;

    @PostConstruct
    public void scheduleNext() {
        scheduleNext(Duration.ofSeconds(1)); // Start almost immediately
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 600000) // Every 10 mins
    public void runRecovery() {
        log.info("Running recovery for stuck ingestion items");
        orchestrator.recoverStuckItems()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    private void scheduleNext(Duration delay) {
        taskScheduler.schedule(() -> 
            Mono.fromRunnable(this::pollEmails)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(), 
            Instant.now().plus(delay));
    }

    public void pollEmails() {
        try {
            if (pollingProperties.isLockEnabled()) {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, lockValue, Duration.ofSeconds(pollingProperties.getLockTtlSeconds()));
                if (acquired == null || !acquired) {
                    log.info("Skipping email poll: lock already held by another instance");
                    return;
                }
            }
            performPoll();
        } finally {
            scheduleNext(calculateNextDelay());
        }
    }

    private void performPoll() {
        long startTime = System.currentTimeMillis();
        int foundCount = 0;
        int processedCount = 0;

        try {
            log.debug("Starting email ingestion poll. Mode: {}", isActiveMode ? "ACTIVE" : "IDLE");
            
            Properties props = new Properties();
            props.put("mail.store.protocol", protocol);
            props.put("mail.imap.ssl.enable", "true");
            // Optimization: Only fetch metadata initially
            props.put("mail.imap.fetchsize", "16384"); 
            
            Session session = Session.getInstance(props);
            try (Store store = session.getStore(protocol)) {
                store.connect(host, port, username, password);
                
                try (Folder folder = store.getFolder(folderName)) {
                    folder.open(Folder.READ_WRITE);
                    
                    Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                    foundCount = messages.length;
                    
                    int toProcess = Math.min(foundCount, pollingProperties.getMaxBatchSize());
                    log.info("Found {} unread messages, processing batch of {}", foundCount, toProcess);
                    
                    if (toProcess > 0) {
                        lastActivityTime = Instant.now();
                        if (!isActiveMode) {
                            log.info("Switching to ACTIVE mode due to new emails");
                            isActiveMode = true;
                        }
                    }

                    // Metadata-first fetch
                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.FLAGS);
                    fp.add(FetchProfile.Item.CONTENT_INFO);
                    fp.add("Message-ID");
                    
                    Message[] batch = new Message[toProcess];
                    System.arraycopy(messages, 0, batch, 0, toProcess);
                    folder.fetch(batch, fp);

                    for (Message message : batch) {
                        try {
                            if (orchestrator.isCandidate((jakarta.mail.internet.MimeMessage) message)) {
                                orchestrator.enqueueProcessing((jakarta.mail.internet.MimeMessage) message);
                                processedCount++;
                            } else {
                                log.info("Message {} is not a candidate, marking as SEEN", message.getMessageNumber());
                            }
                            // Always mark as seen to avoid re-scanning non-candidates
                            message.setFlag(Flags.Flag.SEEN, true);
                        } catch (Exception e) {
                            log.error("Failed to process message {}", message.getMessageNumber(), e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error during email polling", e);
        } finally {
            if (pollingProperties.isLockEnabled()) {
                String currentLock = redisTemplate.opsForValue().get(LOCK_KEY);
                if (lockValue.equals(currentLock)) {
                    redisTemplate.delete(LOCK_KEY);
                }
            }
            
            updateModeAfterPoll(foundCount, startTime);
        }
    }

    private void updateModeAfterPoll(int foundCount, long startTime) {
        // Check if we should revert to IDLE mode
        if (isActiveMode && Instant.now().isAfter(lastActivityTime.plusSeconds(pollingProperties.getActiveModeDurationSeconds()))) {
            log.info("Quiet period exceeded, reverting to IDLE mode");
            isActiveMode = false;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Poll finished in {}ms. Found: {}, Mode: {}", 
                duration, foundCount, isActiveMode ? "ACTIVE" : "IDLE");
    }

    private Duration calculateNextDelay() {
        int baseInterval = isActiveMode ? pollingProperties.getActiveIntervalSeconds() : pollingProperties.getIdleIntervalSeconds();
        
        // Add jitter
        double jitterFactor = 1 + (random.nextDouble() * 2 - 1) * (pollingProperties.getJitterPercentage() / 100.0);
        long nextDelaySeconds = Math.round(baseInterval * jitterFactor);
        if (nextDelaySeconds < 1) nextDelaySeconds = 1;

        log.debug("Next poll in {}s", nextDelaySeconds);
        return Duration.ofSeconds(nextDelaySeconds);
    }
}

package com.isec.platform.modules.certificates.service.ingestion;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailIngestionScheduler {

    private final CertificateIngestionOrchestrator orchestrator;

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

    @Scheduled(fixedDelayString = "${ingestion.email.poll-interval:300000}") // Default 5 mins
    public void pollEmails() {
        log.info("Starting email ingestion poll for {}/{}...", host, username);
        
        Store store = null;
        Folder folder = null;
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", protocol);
            props.put("mail.imap.ssl.enable", "true");
            
            Session session = Session.getInstance(props);
            store = session.getStore(protocol);
            store.connect(host, port, username, password);
            
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_WRITE);
            
            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("Found {} unread messages", messages.length);
            
            for (Message message : messages) {
                try {
                    orchestrator.processEmail((jakarta.mail.internet.MimeMessage) message);
                    message.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception e) {
                    log.error("Failed to process message {}", message.getMessageNumber(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during email polling", e);
        } finally {
            try {
                if (folder != null && folder.isOpen()) folder.close(false);
                if (store != null) store.close();
            } catch (MessagingException e) {
                log.error("Error closing mail store", e);
            }
        }
    }
}

package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.dto.ExtractedCertificateMetadata;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SanlamEmailParser implements EmailParser {

    private static final String SENDER_DOMAIN = "dmvic.com";
    private static final Pattern CERT_PATTERN = Pattern.compile("Certificate #\\s*:\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POLICY_PATTERN = Pattern.compile("Policy #\\s*:\\s*([A-Z0-9/]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REG_PATTERN = Pattern.compile("Vehicle Registration #\\s*:\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHASSIS_PATTERN = Pattern.compile("Chassis #\\s*:\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canParse(MimeMessage message) throws MessagingException {
        return isCandidate(message);
    }

    @Override
    public boolean isSenderValid(String sender) {
        return sender != null && sender.toLowerCase().contains(SENDER_DOMAIN);
    }

    @Override
    public boolean isSubjectMatch(String subject) {
        // Sanlam emails usually have subjects like "Motor Certificate Issued: ..." or "New Certificate Issued - C33981151"
        if (subject == null) return false;
        String lowerSubject = subject.toLowerCase();
        return lowerSubject.contains("certificate") && (lowerSubject.contains("issued") || lowerSubject.contains("new"));
    }

    @Override
    public ExtractedCertificateMetadata parse(MimeMessage message) throws MessagingException, IOException {
        String body = getTextFromMessage(message);

        return ExtractedCertificateMetadata.builder()
                .partnerCode("SANLAM")
                .certificateNumber(extract(CERT_PATTERN, body))
                .policyNumber(extract(POLICY_PATTERN, body))
                .registrationNumber(extract(REG_PATTERN, body))
                .chassisNumber(extract(CHASSIS_PATTERN, body))
                .build();
    }

    @Override
    public String getPartnerCode() {
        return "SANLAM";
    }

    private String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String getTextFromMessage(MimeMessage message) throws MessagingException, IOException {
        String rawText = getTextFromPart(message);
        // Strip HTML tags if present to simplify regex matching
        return rawText.replaceAll("<[^>]*>", " ");
    }

    private String getTextFromPart(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        } else if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                result.append(getTextFromPart(multipart.getBodyPart(i)));
            }
            return result.toString();
        }
        return "";
    }
    
    @Override
    public byte[] extractAttachment(MimeMessage message) throws MessagingException, IOException {
        return extractAttachmentFromPart(message);
    }

    private byte[] extractAttachmentFromPart(Part part) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                byte[] bytes = extractAttachmentFromPart(multipart.getBodyPart(i));
                if (bytes != null) return bytes;
            }
        } else if (part.isMimeType("application/pdf")) {
            try (java.io.InputStream is = part.getInputStream()) {
                return is.readAllBytes();
            }
        }
        return null;
    }
}

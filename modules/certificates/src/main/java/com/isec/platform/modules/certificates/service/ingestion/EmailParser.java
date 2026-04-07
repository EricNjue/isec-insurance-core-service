package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.dto.ExtractedCertificateMetadata;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;

public interface EmailParser {
    boolean canParse(MimeMessage message) throws MessagingException;
    
    default boolean isCandidate(MimeMessage message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0) return false;
        String sender = from[0].toString();
        String subject = message.getSubject();
        return isSenderValid(sender) && isSubjectMatch(subject);
    }

    boolean isSenderValid(String sender);
    boolean isSubjectMatch(String subject);

    ExtractedCertificateMetadata parse(MimeMessage message) throws MessagingException, IOException;
    String getPartnerCode();
    
    default byte[] extractAttachment(MimeMessage message) throws MessagingException, IOException {
        return extractAttachmentFromPart(message);
    }

    private byte[] extractAttachmentFromPart(Part part) throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            jakarta.mail.internet.MimeMultipart multipart = (jakarta.mail.internet.MimeMultipart) part.getContent();
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

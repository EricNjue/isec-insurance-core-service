package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.dto.ExtractedCertificateMetadata;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;

public interface EmailParser {
    boolean canParse(MimeMessage message) throws MessagingException;
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

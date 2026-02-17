package com.isec.platform.modules.ocr.infrastructure.storage;

import com.isec.platform.modules.ocr.config.OcrStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsS3StorageService implements S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final OcrStorageProperties properties;

    @Override
    public String upload(UUID tenantId, MultipartFile file, String logicalPrefix) {
        try {
            String bucket = properties.getBucket();
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalStateException("ocr.storage.bucket is not configured");
            }
            String safeName = sanitizeFilename(file.getOriginalFilename());
            String date = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    .withZone(ZoneId.systemDefault()).format(Instant.now());
            String keyPrefix = (logicalPrefix != null && !logicalPrefix.isBlank()) ? logicalPrefix : properties.getKeyPrefix();
            String key = String.format("%s/%s/%s/%s-%s", keyPrefix, tenantId, date, UUID.randomUUID(), safeName);

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(put, RequestBody.fromBytes(file.getBytes()));

            return "s3://" + bucket + "/" + key;
        } catch (Exception e) {
            log.error("Failed to upload OCR file to S3: {}", e.getMessage(), e);
            throw new IllegalStateException("S3 upload failed", e);
        }
    }

    @Override
    public String generatePresignedUrl(String s3Uri, long expirationMinutes) {
        if (s3Uri == null || !s3Uri.startsWith("s3://")) {
            return s3Uri;
        }
        try {
            String without = s3Uri.substring(5);
            int slash = without.indexOf('/');
            String bucket = without.substring(0, slash);
            String key = without.substring(slash + 1);

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", s3Uri, e.getMessage());
            return s3Uri;
        }
    }

    private String sanitizeFilename(String original) {
        String name = (original == null || original.isBlank()) ? "document" : original;
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

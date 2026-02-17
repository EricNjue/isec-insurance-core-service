package com.isec.platform.modules.ocr.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface S3StorageService {
    /**
     * Uploads the given file to S3 and returns the internal S3 URI (s3://bucket/key).
     */
    String upload(UUID tenantId, MultipartFile file, String logicalPrefix);

    /**
     * Generates a presigned URL for the given S3 URI.
     */
    String generatePresignedUrl(String s3Uri, long expirationMinutes);
}

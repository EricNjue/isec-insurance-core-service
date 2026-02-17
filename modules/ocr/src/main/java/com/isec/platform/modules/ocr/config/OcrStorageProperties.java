package com.isec.platform.modules.ocr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ocr.storage")
public class OcrStorageProperties {
    /** S3 bucket name to store OCR uploads */
    private String bucket;
    /** Optional public base URL (e.g., https://s3.amazonaws.com/<bucket> or minio gateway). If empty, s3:// URL will be returned */
    private String publicBaseUrl;
    /** Optional key prefix under the bucket */
    private String keyPrefix = "uploads/ocr";
}

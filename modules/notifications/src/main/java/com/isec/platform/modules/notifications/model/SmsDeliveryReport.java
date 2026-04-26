package com.isec.platform.modules.notifications.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("sms_delivery_report")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsDeliveryReport {
    @Id
    private UUID id;

    private String messageId;

    private String phoneNumber;

    private String status;

    private String failureReason;

    private Integer retryCount;

    private String networkCode;

    private String rawPayload;

    private LocalDateTime receivedAt;
}

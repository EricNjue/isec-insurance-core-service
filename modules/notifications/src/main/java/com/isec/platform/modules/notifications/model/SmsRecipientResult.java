package com.isec.platform.modules.notifications.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("sms_recipient_result")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRecipientResult {
    @Id
    private UUID id;

    private UUID smsMessageId;

    private String number;

    private String messageId;

    private String status;

    private int statusCode;

    private String cost;

    private String deliveryStatus;

    private String deliveryFailureReason;

    private LocalDateTime deliveryReportedAt;

    private LocalDateTime createdAt;
}

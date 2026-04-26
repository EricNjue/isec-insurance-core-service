package com.isec.platform.modules.notifications.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("sms_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessage {
    @Id
    private UUID id;

    private String recipientTo;

    private String messageContent;

    private String senderFrom;

    private String provider;

    private String providerRequestId;

    private LocalDateTime createdAt;

    private String statusSummary;

    private String totalCost;
}

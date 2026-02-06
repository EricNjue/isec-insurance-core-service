package com.isec.platform.modules.notifications.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sms_recipient_result", indexes = {
    @Index(name = "idx_recipient_number", columnList = "number"),
    @Index(name = "idx_recipient_message_id", columnList = "messageId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRecipientResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sms_message_id", nullable = false)
    private SmsMessage smsMessage;

    @Column(nullable = false)
    private String number;

    private String messageId;

    private String status;

    private int statusCode;

    private String cost;

    private String deliveryStatus;

    private String deliveryFailureReason;

    private LocalDateTime deliveryReportedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

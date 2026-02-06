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
@Table(name = "sms_delivery_report", indexes = {
    @Index(name = "idx_report_message_id", columnList = "message_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsDeliveryReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    private String phoneNumber;

    private String status;

    private String failureReason;

    private Integer retryCount;

    private String networkCode;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;
}

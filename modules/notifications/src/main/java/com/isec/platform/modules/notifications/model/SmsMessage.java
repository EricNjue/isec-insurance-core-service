package com.isec.platform.modules.notifications.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sms_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_to", nullable = false)
    private String to;

    @Column(name = "message_content", nullable = false, length = 1000)
    private String message;

    @Column(name = "sender_from")
    private String from;

    private String provider;

    private String providerRequestId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String statusSummary;

    private String totalCost;

    @OneToMany(mappedBy = "smsMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SmsRecipientResult> recipientResults = new ArrayList<>();

    public void addRecipientResult(SmsRecipientResult result) {
        recipientResults.add(result);
        result.setSmsMessage(this);
    }
}

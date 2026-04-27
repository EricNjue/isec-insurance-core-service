package com.isec.platform.reactive.infra.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    private String type;
    private String payload;
    private OutboxStatus status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastAttemptAt;
    private String errorMessage;
    private String idempotencyKey;

    public enum OutboxStatus {
        PENDING, PROCESSED, FAILED
    }
}

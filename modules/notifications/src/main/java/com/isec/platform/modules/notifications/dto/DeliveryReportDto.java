package com.isec.platform.modules.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryReportDto {
    private String messageId; // maps from 'id' in form
    private String phoneNumber;
    private String status;
    private String failureReason;
    private Integer retryCount;
    private String networkCode;
    private LocalDateTime receivedAt;
}

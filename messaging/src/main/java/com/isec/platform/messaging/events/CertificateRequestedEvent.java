package com.isec.platform.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequestedEvent implements Serializable {
    private String eventId;
    private Long policyId;
    private String policyNumber;
    private String registrationNumber;
    private String certificateType;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String correlationId;
}

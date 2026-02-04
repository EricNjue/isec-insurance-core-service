package com.isec.platform.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuationLetterRequestedEvent implements Serializable {
    private String eventId;
    private Long policyId;
    private String policyNumber;
    private String registrationNumber;
    private String recipientEmail;
    private String correlationId;
}

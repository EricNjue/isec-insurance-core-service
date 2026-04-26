package com.isec.platform.modules.integrations.mpesa.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("mpesa_request_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MpesaRequestLog {
    @Id
    private Long id;

    private String requestType; // STK_PUSH, STK_QUERY, OAUTH

    private String requestPayload;

    private String responsePayload;

    private String checkoutRequestId;

    private String merchantRequestId;

    private String responseCode;

    private LocalDateTime createdAt;
}

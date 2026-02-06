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
public class NotificationSendEvent implements Serializable {
    private String eventId;
    private String recipient;
    private NotificationChannel channel; // EMAIL, SMS
    private String subject;
    private String content;
    private String templateName;
    private String correlationId;
}

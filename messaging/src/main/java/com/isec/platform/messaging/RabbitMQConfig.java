package com.isec.platform.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "insurance.direct.exchange";
    
    // Queue Names
    public static final String APPLICATION_SUBMITTED_QUEUE = "application.submitted.queue";
    public static final String PAYMENT_RECEIVED_QUEUE = "payment.received.queue";
    public static final String CERTIFICATE_REQUESTED_QUEUE = "certificate.requested.queue";
    public static final String NOTIFICATION_SEND_QUEUE = "notification.send.queue";
    
    // Routing Keys
    public static final String APPLICATION_SUBMITTED_RK = "application.submitted";
    public static final String PAYMENT_RECEIVED_RK = "payment.received";
    public static final String CERTIFICATE_REQUESTED_RK = "certificate.requested";
    public static final String NOTIFICATION_SEND_RK = "notification.send";
    public static final String VALUATION_LETTER_REQUESTED_RK = "valuation.letter.requested";
    public static final String CERTIFICATE_ISSUED_RK = "certificate.issued";

    // Dead Letter Queues
    public static final String CERTIFICATE_DLX = EXCHANGE_NAME + ".dlx";
    public static final String CERTIFICATE_DLQ = CERTIFICATE_REQUESTED_QUEUE + ".dlq";
    public static final String NOTIFICATION_DLX = EXCHANGE_NAME + ".notifications.dlx";
    public static final String NOTIFICATION_DLQ = NOTIFICATION_SEND_QUEUE + ".dlq";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(CERTIFICATE_DLX);
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX);
    }

    @Bean
    public Queue certificateRequestedQueue() {
        return QueueBuilder.durable(CERTIFICATE_REQUESTED_QUEUE)
                .withArgument("x-dead-letter-exchange", CERTIFICATE_DLX)
                .withArgument("x-dead-letter-routing-key", CERTIFICATE_REQUESTED_RK + ".dlq")
                .build();
    }

    @Bean
    public Binding certificateRequestedBinding(Queue certificateRequestedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(certificateRequestedQueue).to(exchange).with(CERTIFICATE_REQUESTED_RK);
    }

    @Bean
    public Queue certificateRequestedDlq() {
        return QueueBuilder.durable(CERTIFICATE_DLQ).build();
    }

    @Bean
    public Binding certificateRequestedDlqBinding(Queue certificateRequestedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(certificateRequestedDlq).to(deadLetterExchange).with(CERTIFICATE_REQUESTED_RK + ".dlq");
    }

    @Bean
    public Queue notificationSendQueue() {
        return QueueBuilder.durable(NOTIFICATION_SEND_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_SEND_RK + ".dlq")
                .build();
    }

    @Bean
    public Binding notificationSendBinding(Queue notificationSendQueue, DirectExchange exchange) {
        return BindingBuilder.bind(notificationSendQueue).to(exchange).with(NOTIFICATION_SEND_RK);
    }

    @Bean
    public Queue notificationSendDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding notificationSendDlqBinding(Queue notificationSendDlq, DirectExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationSendDlq).to(notificationDeadLetterExchange).with(NOTIFICATION_SEND_RK + ".dlq");
    }

    @Bean
    public Queue valuationLetterRequestedQueue() {
        return QueueBuilder.durable("valuation.letter.requested.queue").build();
    }

    @Bean
    public Binding valuationLetterRequestedBinding(Queue valuationLetterRequestedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(valuationLetterRequestedQueue).to(exchange).with(VALUATION_LETTER_REQUESTED_RK);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

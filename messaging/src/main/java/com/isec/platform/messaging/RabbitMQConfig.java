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

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue applicationSubmittedQueue() {
        return QueueBuilder.durable(APPLICATION_SUBMITTED_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME + ".dlx")
                .withArgument("x-dead-letter-routing-key", APPLICATION_SUBMITTED_RK + ".dlq")
                .build();
    }

    @Bean
    public Binding applicationSubmittedBinding(Queue applicationSubmittedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(applicationSubmittedQueue).to(exchange).with(APPLICATION_SUBMITTED_RK);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

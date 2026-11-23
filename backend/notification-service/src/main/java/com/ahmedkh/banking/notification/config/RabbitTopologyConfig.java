package com.ahmedkh.banking.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "banking.events.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.events.queue";
    public static final String NOTIFICATION_DLQ = "notification.events.dlq";
    public static final String NOTIFICATION_DLX = "banking.events.dlx";
    public static final String ROUTING_PATTERN = "banking.#";

    @Bean
    public Declarables notificationTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(NOTIFICATION_DLX, true, false);
        Queue dlq = QueueBuilder.durable(NOTIFICATION_DLQ).build();
        Queue queue = QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_PATTERN);
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(NOTIFICATION_DLQ);
        return new Declarables(exchange, dlx, queue, dlq, binding, dlqBinding);
    }
}

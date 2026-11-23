package com.ahmedkh.banking.audit.config;

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
    public static final String AUDIT_QUEUE = "audit.events.queue";
    public static final String AUDIT_DLQ = "audit.events.dlq";
    public static final String AUDIT_DLX = "banking.events.dlx";
    public static final String ROUTING_PATTERN = "banking.#";

    @Bean
    public Declarables auditTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(AUDIT_DLX, true, false);
        Queue dlq = QueueBuilder.durable(AUDIT_DLQ).build();
        Queue queue = QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", AUDIT_DLX)
                .withArgument("x-dead-letter-routing-key", AUDIT_DLQ)
                .build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_PATTERN);
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(AUDIT_DLQ);
        return new Declarables(exchange, dlx, queue, dlq, binding, dlqBinding);
    }
}

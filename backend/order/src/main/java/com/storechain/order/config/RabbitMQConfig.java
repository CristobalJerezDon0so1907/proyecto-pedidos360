package com.storechain.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "orders.exchange";
    public static final String DLX_NAME = "orders.dlx";
    public static final String DLQ_NAME = "orders.dlq";
    public static final String QUEUE_INVENTORY = "inventory.queue";

    // 1. Declarar el Exchange Principal
    @Bean
    public TopicExchange mainExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // 2. Declarar el Dead Letter Exchange (DLX)
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    // 3. Declarar la Dead Letter Queue (DLQ)
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    // 4. Enlazar la DLQ con el DLX
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("deadLetter");
    }

    // 5. Declarar la cola principal con redirección hacia el DLX
    @Bean
    public Queue inventoryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_NAME);
        args.put("x-dead-letter-routing-key", "deadLetter");
        args.put("x-message-ttl", 30000); // 30 segundos TTL
        return new Queue(QUEUE_INVENTORY, true, false, false, args);
    }

    @Bean
    public Binding inventoryBinding() {
        return BindingBuilder.bind(inventoryQueue())
                .to(mainExchange())
                .with("INFO");
    }
}
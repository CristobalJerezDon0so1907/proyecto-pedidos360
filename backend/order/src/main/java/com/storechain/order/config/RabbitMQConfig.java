package com.storechain.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Debe declarar los exchanges y colas con los MISMOS tipos y argumentos que el inventory-service.
 * Si no coinciden, RabbitMQ rechaza la declaracion (PRECONDITION_FAILED) y el servicio que
 * arranca segundo no puede publicar.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "orders.exchange";
    public static final String DLX_NAME = "orders.dlx";
    public static final String DLQ_NAME = "orders.dlq";
    public static final String QUEUE_INVENTORY = "inventory.queue";

    // 1. Exchange principal: DIRECT (igual que inventory-service)
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // 2. Dead Letter Exchange: FANOUT (igual que inventory-service)
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_NAME, true, false);
    }

    // 3. Dead Letter Queue con TTL de 24 horas (igual que inventory-service)
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    // 4. Enlazar la DLQ con el DLX (fanout: sin routing key)
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    // 5. Cola principal: TTL 30 s y luego a la DLQ
    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    // 6. Routing key INFO -> inventory.queue
    @Bean
    public Binding inventoryBinding() {
        return BindingBuilder.bind(inventoryQueue()).to(mainExchange()).with("INFO");
    }
}

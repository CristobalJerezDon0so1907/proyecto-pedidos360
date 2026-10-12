package com.storechain.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDERS_QUEUE = "orders.queue";
    public static final String ORDERS_ROUTING_KEY = "order.created";

    public static final String DLX_EXCHANGE = "orders.dlx";
    public static final String DLQ_QUEUE = "orders.dlq";

    // Direct Exchange Principal
    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE, true, false);
    }

    // Cola Principal con TTL de 30 segundos y redirección a DLX
    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(ORDERS_QUEUE)
                .withArgument("x-message-ttl", 30000) // TTL 30s
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .build();
    }

    @Bean
    public Binding ordersBinding() {
        return BindingBuilder.bind(ordersQueue())
                .to(ordersExchange())
                .with(ORDERS_ROUTING_KEY);
    }

    // Dead Letter Exchange (Fanout)
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_EXCHANGE, true, false);
    }

    // Dead Letter Queue con TTL de 24 Horas
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 86400000) // TTL 24 horas
                .build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange());
    }
}
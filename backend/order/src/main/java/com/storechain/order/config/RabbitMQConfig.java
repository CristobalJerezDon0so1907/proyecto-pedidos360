package com.storechain.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "orders_direct_exchange";
    public static final String EXCHANGE_NAME = "orders_direct_exchange";
    public static final String QUEUE_ALL = "all_orders_queue";
    public static final String QUEUE_CRITICAL = "critical_orders_queue";

    // DLX y DLQ
    public static final String DLX_EXCHANGE = "orders.dlx";
    public static final String DLQ_QUEUE = "orders.dlq";
    public static final String DLX_ROUTING_KEY = "orders.dead";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 horas
                .build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    @Bean
    public Queue allOrdersQueue() {
        return QueueBuilder.durable(QUEUE_ALL)
                .withArgument("x-message-ttl", 30000) // TTL 30 segundos
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue criticalOrdersQueue() {
        return QueueBuilder.durable(QUEUE_CRITICAL)
                .withArgument("x-message-ttl", 30000) // TTL 30 segundos
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindingAllInfo(Queue allOrdersQueue, DirectExchange exchange) {
        return BindingBuilder.bind(allOrdersQueue).to(exchange).with("INFO");
    }

    @Bean
    public Binding bindingAllError(Queue allOrdersQueue, DirectExchange exchange) {
        return BindingBuilder.bind(allOrdersQueue).to(exchange).with("ERROR");
    }

    @Bean
    public Binding bindingCriticalError(Queue criticalOrdersQueue, DirectExchange exchange) {
        return BindingBuilder.bind(criticalOrdersQueue).to(exchange).with("ERROR");
    }
}

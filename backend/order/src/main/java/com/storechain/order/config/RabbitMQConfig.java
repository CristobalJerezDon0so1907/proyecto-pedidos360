package com.storechain.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "orders_direct_exchange";
    public static final String EXCHANGE_NAME = "orders_direct_exchange"; // Compatibilidad con OrderProducerController
    public static final String QUEUE_ALL = "all_orders_queue";
    public static final String QUEUE_CRITICAL = "critical_orders_queue";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue allOrdersQueue() {
        return new Queue(QUEUE_ALL, true);
    }

    @Bean
    public Queue criticalOrdersQueue() {
        return new Queue(QUEUE_CRITICAL, true);
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

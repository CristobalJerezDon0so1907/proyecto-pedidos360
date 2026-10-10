package com.storechain.inventory.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue allOrdersQueue() {
        return new Queue("all_orders_queue", true);
    }
}

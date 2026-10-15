package com.storechain.order.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void createQueue(String queueName, boolean durable) {
        Queue queue = new Queue(queueName, durable, false, false);
        amqpAdmin.declareQueue(queue);
    }

    public boolean deleteQueue(String queueName) {
        return amqpAdmin.deleteQueue(queueName);
    }
}
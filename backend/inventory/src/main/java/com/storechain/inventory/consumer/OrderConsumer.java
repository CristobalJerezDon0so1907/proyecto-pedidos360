package com.storechain.inventory.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {

    @RabbitListener(queues = "all_orders_queue")
    public void receiveAllOrders(String message) {
        System.out.println("[MONITOR GENERAL - INVENTORY] Mensaje recibido de la cola general: " + message);
    }
}

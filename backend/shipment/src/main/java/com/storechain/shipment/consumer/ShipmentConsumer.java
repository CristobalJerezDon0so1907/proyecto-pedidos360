package com.storechain.shipment.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ShipmentConsumer {

    @RabbitListener(queues = "critical_orders_queue")
    public void receiveCriticalOrders(String message) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("[ALERTA CRÍTICA - SHIPMENT] Error detectado en orden: " + message);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
}

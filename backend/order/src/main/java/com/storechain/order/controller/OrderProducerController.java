package com.storechain.order.controller;

import com.storechain.order.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/messages")
@CrossOrigin(origins = "*")
public class OrderProducerController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/send")
    public String sendMessage(@RequestBody Map<String, String> payload) {
        String level = payload.getOrDefault("level", "INFO");
        String message = payload.getOrDefault("message", "Pedido sin mensaje");

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, level, message);
        return "Mensaje enviado a RabbitMQ con Routing Key: " + level;
    }
}

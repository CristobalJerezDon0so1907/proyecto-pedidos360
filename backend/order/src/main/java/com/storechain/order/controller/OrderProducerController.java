package com.storechain.order.controller;

import com.storechain.order.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/messages")
@CrossOrigin(origins = "*")
public class OrderProducerController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/send")
    public String sendOrderMessage(@RequestBody OrderLogMessage message) {
        // Enviar al DirectExchange con la routing key especificada (INFO, WARNING, ERROR)
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                message.getLevel(),
                message.getMessage()
        );
        return "Mensaje enviado a RabbitMQ con Routing Key: " + message.getLevel();
    }

    public static class OrderLogMessage {
        private String level;
        private String message;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}

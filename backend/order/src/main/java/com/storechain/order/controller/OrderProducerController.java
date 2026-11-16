package com.storechain.order.controller;

import com.storechain.order.config.RabbitMQConfig;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/messages")
@CrossOrigin(origins = "*")
public class OrderProducerController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    // INFO -> orders.exchange -> inventory.queue | FAIL -> orders.dlx -> orders.dlq
    @PostMapping("/send")
    public String sendMessage(@RequestBody Map<String, String> payload) {
        String level = payload.getOrDefault("level", "INFO");
        String message = payload.getOrDefault("message", "Pedido sin mensaje");

        if ("FAIL".equalsIgnoreCase(level)) {
            rabbitTemplate.convertAndSend(RabbitMQConfig.DLX_NAME, "deadLetter", message);
            return "Mensaje desviado a la Dead Letter Queue '" + RabbitMQConfig.DLQ_NAME + "' via '" + RabbitMQConfig.DLX_NAME + "'";
        }

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, level, message);
        return "Mensaje enviado a RabbitMQ con Routing Key: " + level;
    }

    // Estado real de las colas (mensajes en espera y consumidores conectados)
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (String cola : new String[]{RabbitMQConfig.QUEUE_INVENTORY, RabbitMQConfig.DLQ_NAME}) {
            QueueInformation info = amqpAdmin.getQueueInfo(cola);
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("mensajes", info != null ? info.getMessageCount() : 0);
            datos.put("consumidores", info != null ? info.getConsumerCount() : 0);
            resultado.put(cola, datos);
        }
        return resultado;
    }
}

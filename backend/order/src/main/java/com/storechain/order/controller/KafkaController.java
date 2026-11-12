package com.storechain.order.controller;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Productor de Kafka para eventos de pedidos.
 *
 * SIN KEY: reparte los mensajes entre las particiones 0, 1, 2 (rotativo).
 *   GET /api/kafka/send?message=Mensaje1
 *
 * CON KEY: Kafka calcula hash(key) -> todos los mensajes con la misma key
 *          van SIEMPRE a la misma partición (orden garantizado).
 *   GET /api/kafka/send?message=Paso1-Creada&key=orden-555
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private static final int PARTICIONES = 3;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicInteger contador = new AtomicInteger();

    @Value("${app.kafka.topic}")
    private String topic;

    public KafkaController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/send")
    public Map<String, Object> sendMessage(@RequestParam String message,
                                           @RequestParam(required = false) String key) throws Exception {
        SendResult<String, String> result;
        String modo;

        if (key != null && !key.isBlank()) {
            // Con key: Kafka elige la partición según hash(key)
            result = kafkaTemplate.send(topic, key, message).get(10, TimeUnit.SECONDS);
            modo = "CON KEY: hash(key) -> siempre la misma particion";
        } else {
            // Sin key: rotamos 0, 1, 2 para ver la distribución (como en la guía)
            int particion = Math.floorMod(contador.getAndIncrement(), PARTICIONES);
            result = kafkaTemplate.send(topic, particion, null, message).get(10, TimeUnit.SECONDS);
            modo = "SIN KEY: reparto rotativo entre particiones";
        }

        RecordMetadata md = result.getRecordMetadata();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("topic", md.topic());
        respuesta.put("particion", md.partition());
        respuesta.put("offset", md.offset());
        respuesta.put("key", (key == null || key.isBlank()) ? "(sin key)" : key);
        respuesta.put("mensaje", message);
        respuesta.put("modo", modo);
        return respuesta;
    }

    @GetMapping("/health")
    public String health() {
        return "Productor Kafka de order-service activo (topic: " + topic + ")";
    }
}
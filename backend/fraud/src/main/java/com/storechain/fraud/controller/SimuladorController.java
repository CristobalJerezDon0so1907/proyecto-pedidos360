package com.storechain.fraud.controller;

import com.storechain.fraud.model.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Simulador de pedidos: publica eventos en el topic de entrada del pipeline.
 *
 *   POST /api/pedidos/transaccion
 *   { "cliente": "juan", "monto": 15000, "canal": "web" }
 */
@RestController
@RequestMapping("/api/pedidos")
public class SimuladorController {

    private final KafkaTemplate<String, Pedido> kafkaTemplate;

    @Value("${app.topics.entrada}")
    private String topicEntrada;

    public SimuladorController(KafkaTemplate<String, Pedido> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/transaccion")
    public String enviarPedido(@RequestBody Pedido pedido) {
        if (pedido.getPedidoId() == null) {
            pedido.setPedidoId("PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (pedido.getTimestamp() == null) {
            pedido.setTimestamp(System.currentTimeMillis());
        }
        // Key = cliente -> todos los pedidos de un cliente van a la misma particion (orden por cliente)
        kafkaTemplate.send(topicEntrada, pedido.getCliente(), pedido);
        return "Enviado al pipeline: " + pedido.getPedidoId() + " | " + pedido.getCliente()
                + " | $" + String.format("%,.0f", pedido.getMonto()) + " CLP";
    }

    @GetMapping("/health")
    public String health() {
        return "fraud-service activo (pipeline Kafka Streams)";
    }
}
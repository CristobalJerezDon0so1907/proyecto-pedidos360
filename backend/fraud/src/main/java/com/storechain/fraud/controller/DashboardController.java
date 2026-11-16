package com.storechain.fraud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// Lee las salidas del pipeline (pedidos-alertas y pedidos-total-cliente) y las expone para el dashboard
@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = {"http://localhost:4200", "https://qk0i5plkz5.execute-api.us-east-1.amazonaws.com"})
public class DashboardController {

    private static final int MAX_ALERTAS = 20;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Deque<JsonNode> alertas = new ConcurrentLinkedDeque<>();
    private final Map<String, Double> totales = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${app.topics.alertas}", groupId = "fraud-dashboard-alertas")
    public void recibirAlerta(String json) {
        try {
            alertas.addFirst(mapper.readTree(json));
            while (alertas.size() > MAX_ALERTAS) {
                alertas.pollLast();
            }
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Alerta con formato invalido: " + json);
        }
    }

    @KafkaListener(topics = "${app.topics.totales}", groupId = "fraud-dashboard-totales")
    public void recibirTotal(ConsumerRecord<String, String> registro) {
        try {
            JsonNode nodo = mapper.readTree(registro.value());
            totales.put(registro.key(), nodo.get("totalClp").asDouble());
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Total con formato invalido: " + registro.value());
        }
    }

    @GetMapping("/alertas")
    public List<JsonNode> obtenerAlertas() {
        return new ArrayList<>(alertas);
    }

    @GetMapping("/totales")
    public Map<String, Double> obtenerTotales() {
        return new TreeMap<>(totales);
    }
}

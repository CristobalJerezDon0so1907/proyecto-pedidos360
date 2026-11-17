package com.storechain.order.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RabbitAdminService {

    // Colas que usan los microservicios: no se pueden borrar desde el dashboard
    public static final Set<String> COLAS_SISTEMA = Set.of(
            "inventory.queue", "orders.dlq", "orders.queue", "all_orders_queue", "critical_orders_queue");

    private final AmqpAdmin amqpAdmin;
    private final RestTemplate rest = new RestTemplate();

    @Value("${app.rabbit.management-url:http://rabbitmq-1:15672}")
    private String managementUrl;

    @Value("${spring.rabbitmq.username}")
    private String usuario;

    @Value("${spring.rabbitmq.password}")
    private String password;

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

    public boolean esColaSistema(String queueName) {
        return COLAS_SISTEMA.contains(queueName);
    }

    // Lista TODAS las colas reales del broker usando la API HTTP de RabbitMQ Management
    public List<Map<String, Object>> listQueues() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(usuario, password);

        List<Map<String, Object>> colas = rest.exchange(
                managementUrl + "/api/queues",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();

        List<Map<String, Object>> resultado = new ArrayList<>();
        if (colas == null) {
            return resultado;
        }
        for (Map<String, Object> cola : colas) {
            String nombre = String.valueOf(cola.get("name"));
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("name", nombre);
            fila.put("mensajes", cola.getOrDefault("messages", 0));
            fila.put("consumidores", cola.getOrDefault("consumers", 0));
            fila.put("sistema", esColaSistema(nombre));
            resultado.add(fila);
        }
        resultado.sort(Comparator.comparing(f -> String.valueOf(f.get("name"))));
        return resultado;
    }
}

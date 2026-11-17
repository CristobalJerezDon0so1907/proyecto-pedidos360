package com.storechain.order.controller;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

// Guia 3.3.2: GET /api/test/start?count=100000 (carga) y GET /api/test/lag (consumer lag)
@RestController
@RequestMapping("/api/test")
public class LoadTestController {

    private static final int MAX_MENSAJES = 500_000;
    private static final int PARTICIONES = 3;
    private static final String GRUPO_ANALYTICS = "analytics-group";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAdmin kafkaAdmin;
    private AdminClient adminClient;

    @Value("${app.kafka.topic-carga}")
    private String topicCarga;

    public LoadTestController(KafkaTemplate<String, String> kafkaTemplate, KafkaAdmin kafkaAdmin) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaAdmin = kafkaAdmin;
    }

    @GetMapping("/start")
    public Map<String, Object> startLoadTest(@RequestParam(defaultValue = "10000") int count) {
        int total = Math.max(1, Math.min(count, MAX_MENSAJES));
        long inicio = System.currentTimeMillis();

        // Carga masiva en paralelo, sin esperar respuesta por mensaje (fire and forget)
        IntStream.range(0, total).parallel().forEach(i -> {
            String camion = "camion-" + (i % 5);
            String mensaje = "{\"despacho\":\"DSP-" + i + "\",\"camion\":\"" + camion
                    + "\",\"lat\":-33.4489,\"lng\":-70.6693,\"payload\":\"Tracking-GPS-Payload-Voluminoso-Para-Llenar-Bytes\"}";
            kafkaTemplate.send(topicCarga, camion, mensaje);
        });
        long msEnvio = System.currentTimeMillis() - inicio;

        // Esperar a que Kafka confirme lo que quedo en el buffer
        kafkaTemplate.flush();
        long msConfirmado = Math.max(1, System.currentTimeMillis() - inicio);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("topic", topicCarga);
        resultado.put("mensajes", total);
        resultado.put("msEnvio", msEnvio);
        resultado.put("msConfirmado", msConfirmado);
        resultado.put("throughputMsgSeg", Math.round(total * 1000.0 / msConfirmado));
        return resultado;
    }

    @GetMapping("/lag")
    public Map<String, Object> lag() throws Exception {
        AdminClient admin = admin();

        Map<TopicPartition, OffsetAndMetadata> leidos = admin.listConsumerGroupOffsets(GRUPO_ANALYTICS)
                .partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);

        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        for (int p = 0; p < PARTICIONES; p++) {
            pedido.put(new TopicPartition(topicCarga, p), OffsetSpec.latest());
        }
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                admin.listOffsets(pedido).all().get(5, TimeUnit.SECONDS);

        long lagTotal = 0;
        List<Map<String, Object>> detalle = new ArrayList<>();
        for (int p = 0; p < PARTICIONES; p++) {
            TopicPartition tp = new TopicPartition(topicCarga, p);
            long fin = finales.get(tp).offset();
            OffsetAndMetadata om = leidos.get(tp);
            long leido = om != null ? om.offset() : 0;
            long lag = Math.max(0, fin - leido);
            lagTotal += lag;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("particion", p);
            fila.put("ultimoOffset", fin);
            fila.put("leido", leido);
            fila.put("lag", lag);
            detalle.add(fila);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("grupo", GRUPO_ANALYTICS);
        resultado.put("topic", topicCarga);
        resultado.put("lagTotal", lagTotal);
        resultado.put("particiones", detalle);
        return resultado;
    }

    // Un solo AdminClient reutilizado (el dashboard consulta el lag cada 3 s)
    private synchronized AdminClient admin() {
        if (adminClient == null) {
            adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        }
        return adminClient;
    }
}

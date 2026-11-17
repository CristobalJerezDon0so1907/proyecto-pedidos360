package com.storechain.shipment.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Guia 3.3.2 - Consumidor LENTO a proposito para provocar Consumer Lag.
 * Simula procesamiento pesado (analitica de rutas): 10 ms por mensaje = ~100 msg/s por hilo.
 * concurrency = consumidores del grupo (maximo util = 3, igual que las particiones).
 * Se cambia con la variable de entorno APP_KAFKA_CARGA_CONCURRENCIA (por defecto 1).
 */
@Service
public class TrackingAnalyticsConsumer {

    private final AtomicLong procesados = new AtomicLong();

    @KafkaListener(topics = "${app.kafka.topic-carga}",
                   groupId = "analytics-group",
                   concurrency = "${app.kafka.carga.concurrencia:1}")
    public void procesar(String mensaje) {
        try {
            Thread.sleep(10); // procesamiento pesado simulado
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long n = procesados.incrementAndGet();
        if (n % 5000 == 0) {
            System.out.println("[ANALYTICS] " + n + " eventos de tracking procesados");
        }
    }
}

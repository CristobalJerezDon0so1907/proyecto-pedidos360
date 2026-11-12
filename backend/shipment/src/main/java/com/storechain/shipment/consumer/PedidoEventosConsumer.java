package com.storechain.shipment.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Consumidor de Kafka del shipment-service.
 * Escucha el topic 'pedidos-eventos' y muestra en qué partición y offset
 * llegó cada mensaje, y con qué key.
 *
 * Grupo 'shipment-grupo': Kafka recuerda hasta qué offset leyó este grupo,
 * así que si el servicio se reinicia, NO vuelve a leer los mensajes ya procesados.
 */
@Service
public class PedidoEventosConsumer {

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "shipment-grupo")
    public void escuchar(@Payload String mensaje,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                         @Header(KafkaHeaders.OFFSET) long offset,
                         @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {

        System.out.println("\n" + "=".repeat(80));
        System.out.println(" [SHIPMENT] EVENTO DE PEDIDO RECIBIDO DESDE KAFKA");
        System.out.printf("  Particion : %d%n", particion);
        System.out.printf("  Offset    : %d%n", offset);
        System.out.printf("  Key       : %s%n", key != null ? key : "(sin key)");
        System.out.printf("  Mensaje   : %s%n", mensaje);
        System.out.println("=".repeat(80) + "\n");
    }
}
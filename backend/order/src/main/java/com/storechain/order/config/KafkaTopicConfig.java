package com.storechain.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Crea el topic 'pedidos-eventos' con 3 particiones al arrancar el servicio.
 * (Kafka tiene la auto-creación de topics desactivada, así que lo declaramos aquí).
 * Replication factor 1 porque hay un solo broker (en producción serían 2-3).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic pedidosEventosTopic(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
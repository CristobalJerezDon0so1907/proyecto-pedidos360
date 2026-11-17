package com.storechain.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Guia 3.2.1: topic de eventos de pedidos (3 particiones)
    @Bean
    public NewTopic pedidosEventosTopic(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    // Guia 3.3.2: tracking GPS de despachos con RETENCION agresiva
    @Bean
    public NewTopic trackingDespachosTopic(@Value("${app.kafka.topic-carga}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "300000")      // borrar mensajes de mas de 5 minutos
                .config(TopicConfig.RETENTION_BYTES_CONFIG, "52428800") // o si una particion supera 50 MB
                .config(TopicConfig.SEGMENT_MS_CONFIG, "60000")         // cerrar un segmento cada 1 minuto
                .config(TopicConfig.SEGMENT_BYTES_CONFIG, "10485760")   // o cada 10 MB (solo se borran segmentos cerrados)
                .build();
    }
}

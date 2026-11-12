package com.storechain.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

/**
 * Fraud-service de Pedidos360.
 * Pipeline de eventos con Kafka Streams: lee pedidos, filtra, enriquece,
 * detecta pedidos sospechosos, agrega el total por cliente y publica alertas.
 */
@SpringBootApplication
@EnableKafkaStreams
public class FraudApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudApplication.class, args);
    }
}
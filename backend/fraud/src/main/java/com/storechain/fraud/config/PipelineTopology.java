package com.storechain.fraud.config;

import com.storechain.fraud.model.AlertaPedido;
import com.storechain.fraud.model.Pedido;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

/**
 * PIPELINE DE EVENTOS (Kafka Streams)
 *
 *  pedidos-transacciones -> [1. Filtro] -> [2. Enriquecimiento] -+-> [3. Deteccion]  -> pedidos-alertas
 *                                                                +-> [4. Agregacion] -> pedidos-total-cliente
 */
@Configuration
public class PipelineTopology {

    private static final double MONTO_MINIMO_CLP = 1_000;       // menos que esto se ignora
    private static final double UMBRAL_MEDIA_CLP = 500_000;     // > 500 mil  -> MEDIA
    private static final double UMBRAL_ALTA_CLP  = 1_000_000;   // > 1 millon -> ALTA
    private static final double CLP_POR_USD      = 950.0;       // tipo de cambio simulado

    @Value("${app.topics.entrada}") private String topicEntrada;
    @Value("${app.topics.alertas}") private String topicAlertas;
    @Value("${app.topics.totales}") private String topicTotales;

    @Bean
    public KStream<String, Pedido> pipelinePedidos(StreamsBuilder builder) {

        // Serializadores JSON (sin cabeceras de tipo, para que se lean limpios en Kafka UI)
        JsonSerde<Pedido> pedidoSerde = new JsonSerde<>(Pedido.class).noTypeInfo();
        JsonSerde<AlertaPedido> alertaSerde = new JsonSerde<>(AlertaPedido.class).noTypeInfo();

        // PASO 0: LEER (Source) - key = cliente
        KStream<String, Pedido> entrada = builder.stream(
                topicEntrada, Consumed.with(Serdes.String(), pedidoSerde));

        // PASO 1: FILTRAR - ignorar pedidos invalidos o menores a $1.000 CLP
        KStream<String, Pedido> validos = entrada
                .filter((cliente, p) -> p != null && p.getMonto() != null && p.getMonto() >= MONTO_MINIMO_CLP)
                .peek((cliente, p) -> System.out.printf(
                        "[PIPELINE] Pedido procesado: %s | cliente=%s | $%,.0f CLP | canal=%s%n",
                        p.getPedidoId(), cliente, p.getMonto(), p.getCanal()));

        // PASO 2 y 3: ENRIQUECER (CLP -> USD) y DETECTAR pedidos sospechosos
        KStream<String, AlertaPedido> alertas = validos
                .filter((cliente, p) -> p.getMonto() > UMBRAL_MEDIA_CLP)
                .mapValues(p -> {
                    boolean alta = p.getMonto() > UMBRAL_ALTA_CLP;
                    double usd = Math.round(p.getMonto() / CLP_POR_USD * 100.0) / 100.0;
                    return new AlertaPedido(
                            p.getPedidoId(),
                            p.getCliente(),
                            p.getMonto(),
                            usd,
                            (alta ? "Pedido inusualmente alto" : "Pedido sobre el monto habitual")
                                    + " en canal " + p.getCanal(),
                            alta ? "ALTA" : "MEDIA");
                })
                .peek((cliente, a) -> System.out.printf(
                        "[PIPELINE] >>> ALERTA %s: %s | cliente=%s | US$%,.2f%n",
                        a.getSeveridad(), a.getMotivo(), cliente, a.getMontoUsd()));

        // SALIDA 1 (Sink): alertas al topic pedidos-alertas
        alertas.to(topicAlertas, Produced.with(Serdes.String(), alertaSerde));

        // PASO 4: AGREGAR - total gastado por cliente en tiempo real (KTable)
        KTable<String, Double> totalPorCliente = validos
                .groupByKey(Grouped.with(Serdes.String(), pedidoSerde))
                .aggregate(
                        () -> 0.0,
                        (cliente, p, total) -> total + p.getMonto(),
                        Materialized.with(Serdes.String(), Serdes.Double()));

        // SALIDA 2 (Sink): total por cliente al topic pedidos-total-cliente
        totalPorCliente.toStream()
                .peek((cliente, total) -> System.out.printf(
                        "[PIPELINE] Total acumulado de %s: $%,.0f CLP%n", cliente, total))
                .mapValues(total -> String.format("{\"totalClp\": %.0f}", total))
                .to(topicTotales, Produced.with(Serdes.String(), Serdes.String()));

        return entrada;
    }
}
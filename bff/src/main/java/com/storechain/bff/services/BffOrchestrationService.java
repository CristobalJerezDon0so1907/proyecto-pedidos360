package com.storechain.bff.service;

import com.storechain.bff.dto.CheckoutRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class BffOrchestrationService {

    private final WebClient orderClient;
    private final WebClient inventoryClient;
    private final WebClient shipmentClient;

    public BffOrchestrationService(
            @Qualifier("orderWebClient") WebClient orderClient,
            @Qualifier("inventoryWebClient") WebClient inventoryClient,
            @Qualifier("shipmentWebClient") WebClient shipmentClient) {
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.shipmentClient = shipmentClient;
    }

    public Mono<Map<String, Object>> processCheckout(CheckoutRequest request) {
        // 1. Reservar o Validar Stock en el microservicio de Inventario
        return inventoryClient.post()
                .uri("/api/inventory/reserve")
                .bodyValue(request.getItems())
                .retrieve()
                .bodyToMono(Boolean.class)
                .flatMap(stockAvailable -> {
                    if (!Boolean.TRUE.equals(stockAvailable)) {
                        return Mono.error(new RuntimeException("Stock insuficiente para procesar la orden."));
                    }
                    // 2. Crear Orden en el microservicio de Orders
                    return orderClient.post()
                            .uri("/api/orders")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(Map.class);
                })
                .flatMap(orderResult -> {
                    // 3. Crear EnvÃo / Despacho en el microservicio de Shipments
                    Map<String, Object> shipmentPayload = new HashMap<>();
                    shipmentPayload.put("orderId", orderResult.get("id"));
                    shipmentPayload.put("address", request.getShippingAddress());

                    return shipmentClient.post()
                            .uri("/api/shipments")
                            .bodyValue(shipmentPayload)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(shipmentResult -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("order", orderResult);
                                response.put("shipment", shipmentResult);
                                response.put("status", "SUCCESS");
                                return response;
                            });
                });
    }
}
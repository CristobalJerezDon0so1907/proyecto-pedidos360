package com.storechain.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.order.url:http://localhost:8081}")
    private String orderServiceUrl;

    @Value("${services.inventory.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    @Value("${services.shipment.url:http://localhost:8083}")
    private String shipmentServiceUrl;

    @Bean
    public WebClient orderWebClient(WebClient.Builder builder) {
        return builder.baseUrl(orderServiceUrl).build();
    }

    @Bean
    public WebClient inventoryWebClient(WebClient.Builder builder) {
        return builder.baseUrl(inventoryServiceUrl).build();
    }

    @Bean
    public WebClient shipmentWebClient(WebClient.Builder builder) {
        return builder.baseUrl(shipmentServiceUrl).build();
    }
}
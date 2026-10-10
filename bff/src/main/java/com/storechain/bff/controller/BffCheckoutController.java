package com.storechain.bff.controller;

import com.storechain.bff.dto.CheckoutRequest;
import com.storechain.bff.service.BffOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/bff/checkout")
public class BffCheckoutController {

    private final BffOrchestrationService orchestrationService;

    public BffCheckoutController(BffOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> checkout(@RequestBody CheckoutRequest request) {
        return orchestrationService.processCheckout(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }
}
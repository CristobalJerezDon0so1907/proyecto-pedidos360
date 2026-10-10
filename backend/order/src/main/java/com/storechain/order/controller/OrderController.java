package com.storechain.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderRequest) {
        Map<String, Object> createdOrder = new HashMap<>(orderRequest);
        createdOrder.put("id", UUID.randomUUID().toString());
        createdOrder.put("status", "CREATED");
        
        return ResponseEntity.ok(createdOrder);
    }
}
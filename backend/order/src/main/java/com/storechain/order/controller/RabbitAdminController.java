package com.storechain.order.controller;

import com.storechain.order.dto.CreateQueueRequest;
import com.storechain.order.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/queues")
@CrossOrigin(origins = "*")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<?> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        try {
            rabbitAdminService.createQueue(request.getName(), request.isDurable());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "SUCCESS",
                    "message", "Cola creada exitosamente: " + request.getName(),
                    "durable", request.isDurable()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Error al crear la cola: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{queueName}")
    public ResponseEntity<?> deleteQueue(@PathVariable String queueName) {
        try {
            boolean deleted = rabbitAdminService.deleteQueue(queueName);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Cola eliminada exitosamente: " + queueName
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "NOT_FOUND",
                        "message", "La cola no existe o no pudo ser eliminada: " + queueName
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Error al eliminar la cola: " + e.getMessage()
            ));
        }
    }
}
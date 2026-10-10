package com.storechain.shipment.controller;

import com.storechain.shipment.entities.Shipment;
import com.storechain.shipment.repository.ShipmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipment")
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;

    public ShipmentController(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@RequestBody Shipment shipment) {
        shipment.setStatus("PENDING");
        shipment.setTrackingCode("TRACK-" + System.currentTimeMillis());
        return ResponseEntity.ok(shipmentRepository.save(shipment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Shipment> getShipmentByOrder(@PathVariable Long orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

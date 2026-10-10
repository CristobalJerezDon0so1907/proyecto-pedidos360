package com.storechain.inventory.controller;

import com.storechain.inventory.entities.InventoryItem;
import com.storechain.inventory.repository.InventoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItem> getStock(@PathVariable String productId) {
        return inventoryRepository.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new InventoryItem(productId, 100)));
    }
}

package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.StoreRequest;
import org.fleet.backend.dto.StoreResponse;
import org.fleet.backend.entity.Store;
import org.fleet.backend.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    //PUBLIC ENDPOINTS (Customer + Rider)
    //Get all stores with pin colors (RED/ORANGE/GREEN)For map display
    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores() {
        return ResponseEntity.ok(storeService.getStoresWithStatus());
    }

    //Get nearby stores within radius
    @GetMapping("/nearby")
    public ResponseEntity<List<Store>> getNearbyStores(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        return ResponseEntity.ok(storeService.getNearbyStores(lat, lng, radiusKm));
    }

    //Get store by ID
    @GetMapping("/{id}")
    public ResponseEntity<Store> getStoreById(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    //ADMIN ENDPOINTS
    //Admin adds a new store
    @PostMapping("/admin/stores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Store> addStore(@Valid @RequestBody StoreRequest request) {
        Store store = storeService.addStore(request);
        return ResponseEntity.ok(store);
    }

    //Admin updates a store
    @PutMapping("/admin/stores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Store> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody StoreRequest request) {
        Store store = storeService.updateStore(id, request);
        return ResponseEntity.ok(store);
    }

    //Admin deactivates a store (soft delete)
    @DeleteMapping("/admin/stores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateStore(@PathVariable Long id) {
        storeService.deactivateStore(id);
        return ResponseEntity.ok("Store deactivated successfully");
    }

    //Admin gets all stores (including inactive)
    @GetMapping("/admin/stores/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Store>> getAllStoresForAdmin() {
        return ResponseEntity.ok(storeService.getAllStoresForAdmin());
    }
}
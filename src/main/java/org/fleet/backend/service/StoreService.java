package org.fleet.backend.service;

import org.fleet.backend.dto.StoreRequest;
import org.fleet.backend.dto.StoreResponse;
import org.fleet.backend.entity.*;
import org.fleet.backend.repository.OrderRepository;
import org.fleet.backend.repository.StoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    public StoreService(OrderRepository orderRepository, StoreRepository storeRepository) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
    }

    //Add store (Admin)
    public Store addStore(StoreRequest request){
        Store store =  new Store();
        store.setName(request.name());
        store.setAddress(request.address());
        store.setCity(request.city());
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
        store.setIsActive(true);

        return storeRepository.save(store);
    }

    // 2. Update store (Admin only)
    public Store updateStore(Long id, StoreRequest request) {
        Store store = getStoreById(id);
        store.setName(request.name());
        store.setAddress(request.address());
        store.setCity(request.city());
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
        return storeRepository.save(store);
    }

    // 3. Deactivate store (Admin only)
    public void deactivateStore(Long id) {
        Store store = getStoreById(id);
        store.setIsActive(false);
        storeRepository.save(store);
    }

    public List<Store> getAllActiveStores() {
        return storeRepository.findByIsActiveTrue();
    }

    public Store getStoreById(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Store not found"));
    }

    // 6. Get stores with pin color and status (for map)
    public List<StoreResponse> getStoresWithStatus() {
        List<Store> stores = getAllActiveStores();

        return stores.stream()
                .map(store -> {
                    PinColor pinColor = determinePinColor(store);
                    StoreStatus status = determineStoreStatus(store);
                    Long activeOrderId = getActiveOrderId(store);

                    return new StoreResponse(
                            store.getId(),
                            store.getName(),
                            store.getAddress(),
                            store.getCity(),
                            store.getLatitude(),
                            store.getLongitude(),
                            store.getIsActive(),
                            pinColor,
                            status,
                            activeOrderId
                    );
                })
                .collect(Collectors.toList());
    }

    // 7. Get nearby stores
    public List<Store> getNearbyStores(Double lat, Double lng, Double radiusKm) {
        return storeRepository.findNearbyStores(lat, lng, radiusKm);
    }

    public List<Store> getAllStoresForAdmin() {
        return storeRepository.findAll();
    }

    // Helper methods
    private PinColor determinePinColor(Store store) {
        // Check if store is pickup for any active order
        if (orderRepository.existsByPickupStoreAndOrderStatusNot(store, OrderStatus.PAYMENT_RELEASED)) {
            return PinColor.ORANGE;
        }
        // Check if store is dropoff for any active order
        if (orderRepository.existsByDropoffStoreAndOrderStatusNot(store, OrderStatus.PAYMENT_RELEASED)) {
            return PinColor.GREEN;
        }
        return PinColor.RED;
    }

    private StoreStatus determineStoreStatus(Store store) {
        if (orderRepository.existsByPickupStoreAndOrderStatusNot(store, OrderStatus.PAYMENT_RELEASED)) {
            return StoreStatus.PICKUP;
        }
        if (orderRepository.existsByDropoffStoreAndOrderStatusNot(store, OrderStatus.PAYMENT_RELEASED)) {
            return StoreStatus.DROPOFF;
        }
        return StoreStatus.AVAILABLE;
    }

    private Long getActiveOrderId(Store store) {
        return orderRepository.findActiveOrderByStore(store, OrderStatus.PAYMENT_RELEASED)
                .map(Order::getId)
                .orElse(null);
    }
}


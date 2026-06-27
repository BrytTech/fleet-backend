package org.fleet.backend.entity;

public enum StoreStatus {
    AVAILABLE,  // No active order
    PICKUP,     // Store is pickup for an active order
    DROPOFF,    // Store is dropoff for an active order
    COMPLETED
}

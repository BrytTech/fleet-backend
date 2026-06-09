package org.fleet.backend.entity;

public enum OrderStatus {

    PENDING,      // Waiting for rider
    ASSIGNED,     // Rider accepted
    PICKED_UP,    // Rider picked up package
    IN_TRANSIT,   // On the way
    DELIVERED,    // Successfully delivered
    CANCELLED     // Cancelled by customer or rider
}

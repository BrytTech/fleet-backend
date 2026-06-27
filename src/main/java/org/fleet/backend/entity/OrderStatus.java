package org.fleet.backend.entity;

public enum OrderStatus {

    PENDING,           // Waiting for rider
    ASSIGNED,          // Rider accepted
    PICKED_UP,         // Rider picked up package
    IN_TRANSIT,        // On the way
    DELIVERED,         // Successfully delivered
    CUSTOMER_CONFIRMED, // Customer verified delivery
    PAYMENT_RELEASED,   // Payment sent to rider
    CANCELLED
}

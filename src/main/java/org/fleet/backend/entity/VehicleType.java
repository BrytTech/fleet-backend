package org.fleet.backend.entity;

public enum VehicleType {
    MOTORBIKE,
    CAR,
    VAN,
    TRUCK;

    // Helper method to get display name
    public String getDisplayName() {
        return switch (this) {
            case MOTORBIKE -> "Motorbike";
            case CAR -> "Car";
            case VAN -> "Van";
            case TRUCK -> "Truck";
        };
    }

    // Helper method to get price multiplier
    public double getPriceMultiplier() {
        return switch (this) {
            case MOTORBIKE -> 1.0;
            case CAR -> 1.5;
            case VAN -> 2.0;
            case TRUCK -> 3.0;
        };
    }
}

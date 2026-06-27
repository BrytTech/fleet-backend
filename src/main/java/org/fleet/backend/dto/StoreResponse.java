package org.fleet.backend.dto;

import org.fleet.backend.entity.PinColor;
import org.fleet.backend.entity.StoreStatus;

public record StoreResponse(
        Long id,
        String name,
        String address,
        String city,
        Double latitude,
        Double longitude,
        Boolean isActive,
        PinColor pinColor,
        StoreStatus status,
        Long activeOrderId
        ) {
}

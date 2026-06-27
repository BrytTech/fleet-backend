package org.fleet.backend.dto;

public record StoreRequest(
        String name,
        String address,
        String city,
        Double latitude,
        Double longitude

) {
}

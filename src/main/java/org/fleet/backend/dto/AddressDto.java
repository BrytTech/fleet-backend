package org.fleet.backend.dto;

/**
 * A place a rider goes, given directly rather than as a Fleet store.
 *
 * <p>The customer app already sends this shape for a pickup or drop-off that is
 * not one of our stores, and it is the same shape TradePay's client builds, so
 * it is the contract all three sides had already converged on.
 *
 * <p>Coordinates are what the price and the route are computed from; the text
 * line is for the rider to read at the door. {@code recipientName} and
 * {@code recipientPhone} are meaningful at the drop-off end and ignored at
 * pickup, which is why they live here rather than in two near-identical types.
 */
public record AddressDto(
        String recipientName,
        String recipientPhone,
        String addressLine,
        String city,
        String region,
        Double latitude,
        Double longitude
) {
    /** True when this address can actually be routed to. */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}

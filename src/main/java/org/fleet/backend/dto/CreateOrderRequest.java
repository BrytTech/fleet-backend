package org.fleet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.fleet.backend.entity.VehicleType;

import java.math.BigDecimal;

/**
 * A request to price or book a delivery.
 *
 * <p>Each end of the journey is given <em>either</em> as a Fleet store id or as
 * a loose address with coordinates, never both. The in-app flow moves parcels
 * between stores; a B2B partner moves them from a seller's shop to a buyer's
 * home, and neither of those is a store Fleet knows about.
 */
public record CreateOrderRequest(
        Long pickupStoreId,

        Long dropoffStoreId,

        // Used when the corresponding store id is absent. Both carry coordinates;
        // without them there is nothing to price or route by.
        AddressDto pickupAddress,

        AddressDto dropoffAddress,

        @NotBlank(message = "Please provide package description")
        String packageDescription,

        @NotNull(message = "Package weight is required")
        @Positive(message = "Weight must be positive")
        BigDecimal packageWeight,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        String recipientName,
        String recipientPhone,
        String senderName,
        String senderPhone,
        Object packagePhotos,

        /**
         * The caller's own reference for this delivery. Repeating one returns the
         * order it already created rather than booking a second rider.
         */
        String externalOrderId
) {
}

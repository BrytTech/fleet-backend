package org.fleet.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //USER RELATIONSHIPS
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"customerOrders", "riderOrders"})
    private CustomerProfile customer;

    @ManyToOne
    @JoinColumn(name = "rider_id")
    @JsonIgnoreProperties({"customerOrders", "riderOrders"})
    private RiderProfile rider;

    //ORDER IDENTIFIER
    @Column(nullable = false, unique = true)
    private String orderNumber;

    //STORE REFERENCES
    // Nullable since B2B partners deliver between places that are not Fleet
    // stores: a marketplace seller's shop on one end and a buyer's home on the
    // other. Each end is either a store reference or the loose address below,
    // never both and never neither — OrderService enforces that.
    @ManyToOne
    @JoinColumn(name = "pickup_store_id")
    private Store pickupStore;

    @ManyToOne
    @JoinColumn(name = "dropoff_store_id")
    private Store dropoffStore;

    //AD-HOC ADDRESSES (used when the corresponding store reference is null)
    // Coordinates are what the price and the rider's route are built from; the
    // text line is for the rider to read at the door, not to navigate by.
    private String pickupAddress;
    private String pickupCity;
    private Double pickupLatitude;
    private Double pickupLongitude;

    private String dropoffAddress;
    private String dropoffCity;
    private Double dropoffLatitude;
    private Double dropoffLongitude;

    /**
     * The partner's own id for this delivery.
     *
     * <p>Unique so that a retried booking after a network timeout returns the
     * order already created instead of sending a second rider and billing the
     * partner twice. Null for orders placed by ordinary customers in the app.
     */
    @Column(unique = true)
    private String externalOrderId;

    /**
     * How many events we have published about this order.
     *
     * <p>Rides along on every partner callback so the receiver can discard one
     * that arrives out of order — a retried PICKED_UP landing after DELIVERED
     * would otherwise walk the parcel backwards.
     */
    @Column(nullable = false, columnDefinition = "bigint not null default 0")
    private long webhookSeq = 0;

    //PACKAGE DETAILS
    private String packageDescription;
    private BigDecimal packageWeight;
    private BigDecimal distance;
    private BigDecimal price;

    private String recipientName;
    private String recipientPhone;
    private String senderName;
    private String senderPhone;

    @Column(columnDefinition = "TEXT")
    private String packagePhotos;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    //QR CODE (NEW)
    @Column(unique = true)
    private String qrCode;

    //STATUS
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_url")
    private String paymentUrl;

    @Column(name = "payment_session_id")
    private String paymentSessionId;

    private String paymentReference;

    //TIMESTAMPS
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    //QR SCAN TIMESTAMPS (NEW)
    private LocalDateTime riderPickupScannedAt;
    private LocalDateTime riderDropoffScannedAt;
    private LocalDateTime customerConfirmedAt;

    //PAYMENT RELEASE (NEW)
    private boolean isPaymentReleased = false;

    //CANCELLATION
    @Enumerated(EnumType.STRING)
    private Role cancelledBy;
}
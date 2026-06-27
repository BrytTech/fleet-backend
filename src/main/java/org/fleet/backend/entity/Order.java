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

    //STORE REFERENCES (NEW)
    @ManyToOne
    @JoinColumn(name = "pickup_store_id", nullable = false)
    private Store pickupStore;

    @ManyToOne
    @JoinColumn(name = "dropoff_store_id", nullable = false)
    private Store dropoffStore;

    //PACKAGE DETAILS
    private String packageDescription;
    private BigDecimal packageWeight;
    private BigDecimal distance;
    private BigDecimal price;

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
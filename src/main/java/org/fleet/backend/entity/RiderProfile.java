package org.fleet.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rider_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Relationships
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"customerProfile", "riderProfile", "password"})
    private User user;

    @OneToMany(mappedBy = "rider")
    @JsonBackReference
    private List<Order> riderOrders = new ArrayList<>();

    @Column(name = "vehicle_type")
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Column(name = "vehicle_plate")
    private String vehiclePlate;

    @Column(name = "ghanacard_number", unique = true)
    private String ghanaCardNumber;

    @Column(name = "ghanacard_front_image")
    private String ghanaCardFrontImageUrl;

    @Column(name = "ghanacard_back_image")
    private String ghanaCardBackImageUrl;

    @Column(name = "driving_license_number", unique = true)
    private String drivingLicenseNumber;

    @Column(name = "driving_license_front_image")
    private String drivingLicenseFrontImageUrl;

    @Column(name = "driving_license_back_image")
    private String drivingLicenseBackImageUrl;

    @Column(name = "verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "total_earnings")
    private Double totalEarnings = 0.0;

    @Column(name = "completed_deliveries")
    private Integer completedDeliveries = 0;


}

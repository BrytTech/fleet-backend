package org.fleet.backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String password;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isActive;

    //Relationship fields
    @OneToMany(mappedBy = "customer")
    @JsonManagedReference
    private List<Order> customerOrders = new ArrayList<>();

    @OneToMany(mappedBy = "rider")
    @JsonManagedReference
    private List<Order> riderOrders = new ArrayList<>();

    // Rider verification fields
    @Column(unique = true)
    private String idCardNumber;         // Ghana Card
    private String idCardImageUrl;

    @Column(unique = true)
    private String drivingLicenseNumber;   // Driving License
    private String drivingLicenseImageUrl;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;       // PENDING, APPROVED, REJECTED

    private String rejectionReason;
    private LocalDateTime verifiedAt;

}

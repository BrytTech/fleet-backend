package org.fleet.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", unique = true)
    private String phone;

    @Column(name = "password")
    private String password;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_active")
    private boolean isActive;

    /**
     * A B2B partner account — a business booking deliveries through the API
     * rather than a person booking one in the app.
     *
     * <p>Two things follow from it. A partner's order is settled on account
     * instead of through an Aza checkout page, because there is nobody at a
     * screen to pay one; and because riders are only offered orders that are
     * already paid, a partner order that waited for a checkout would never
     * reach a rider at all.
     */
    // The default is part of the column definition, not just the field: adding a
    // plain NOT NULL column to a table that already has rows fails, and every
    // existing account predates this flag.
    @Column(name = "is_partner", nullable = false, columnDefinition = "boolean not null default false")
    private boolean partner = false;

    //Relationship fields for profiles
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private RiderProfile riderProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private CustomerProfile customerProfile;
}

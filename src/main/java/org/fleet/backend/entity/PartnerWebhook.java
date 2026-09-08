package org.fleet.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Where a B2B partner wants to be told about their deliveries.
 *
 * <p>A partner cannot poll us usefully: they need to know a parcel was delivered
 * at the moment it happens, because that is when they release the seller's money.
 * So we call them.
 *
 * <p>The secret is theirs, not ours — they generate it, we sign with it, and they
 * verify the signature to know the callback really came from Fleet. Anyone who
 * can reach their endpoint can otherwise claim a parcel was delivered, which on a
 * marketplace means claiming money should move.
 */
@Entity
@Table(name = "partner_webhooks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** HTTPS endpoint we POST signed events to. */
    @Column(nullable = false, length = 500)
    private String url;

    /** Shared HMAC-SHA256 signing secret. Never returned by any endpoint. */
    @Column(nullable = false, length = 200)
    private String secret;

    /** The partner account these events belong to. */
    @ManyToOne
    @JoinColumn(name = "partner_user_id")
    private User partner;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

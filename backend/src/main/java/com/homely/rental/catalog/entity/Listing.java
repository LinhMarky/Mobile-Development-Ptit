package com.homely.rental.catalog.entity;

import com.homely.rental.common.entity.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Listing entity (BR-02).
 * A listing is a published advertisement for a room.
 * Lifecycle: DRAFT → PENDING_REVIEW → PUBLISHED → HIDDEN/EXPIRED/ARCHIVED
 */
@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Listing extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "title", length = 150, nullable = false)
    private String title = "";

    @Column(name = "description", length = 5000, nullable = false)
    private String description = "";

    /** Monthly rent in VND */
    @Column(name = "rent_vnd", precision = 18, scale = 0)
    private BigDecimal rentVnd;

    /** Deposit amount in VND */
    @Column(name = "deposit_vnd", precision = 18, scale = 0)
    private BigDecimal depositVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ListingStatus status = ListingStatus.DRAFT;

    /** Snapshot of room terms_version at time of publish */
    @Column(name = "terms_version", nullable = false)
    private int termsVersion = 1;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Version
    private int version;
}

package com.homely.rental.booking.entity;

import com.homely.rental.auth.entity.User;
import com.homely.rental.catalog.entity.Listing;
import com.homely.rental.catalog.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Booking entity (§2.5, BR-06).
 * Tracks the full lifecycle: PENDING → APPROVED → CONFIRMED → COMPLETED.
 */
@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "desired_move_in")
    private LocalDate desiredMoveIn;

    @Column(name = "occupant_count", nullable = false)
    private int occupantCount = 1;

    @Column(name = "rent_vnd", precision = 18, scale = 0, nullable = false)
    private BigDecimal rentVnd;

    @Column(name = "deposit_vnd", precision = 18, scale = 0, nullable = false)
    private BigDecimal depositVnd = BigDecimal.ZERO;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "terms_snapshot", columnDefinition = "JSON", nullable = false)
    private String termsSnapshot;

    @Column(name = "snapshot_schema_version", nullable = false)
    private int snapshotSchemaVersion = 1;

    @Column(name = "request_expires_at", nullable = false)
    private Instant requestExpiresAt;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "handover_due_at")
    private Instant handoverDueAt;

    @Column(name = "tenant_terms_accepted_at")
    private Instant tenantTermsAcceptedAt;

    @Column(name = "tenant_handover_confirmed_at")
    private Instant tenantHandoverConfirmedAt;

    @Column(name = "host_handover_confirmed_at")
    private Instant hostHandoverConfirmedAt;

    @Column(name = "allocated_payment_id", unique = true)
    private Long allocatedPaymentId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "last_reason", length = 1000)
    private String lastReason;

    @Version
    private int version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

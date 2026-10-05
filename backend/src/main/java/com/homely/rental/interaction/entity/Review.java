package com.homely.rental.interaction.entity;

import com.homely.rental.auth.entity.User;
import com.homely.rental.catalog.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One review per booking — UNIQUE constraint in DB */
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "rating", nullable = false)
    private int rating; // 1–5

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Column(name = "hide_reason", length = 500)
    private String hideReason;

    @Version
    private int version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

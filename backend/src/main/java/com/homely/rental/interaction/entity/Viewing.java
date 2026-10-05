package com.homely.rental.interaction.entity;

import com.homely.rental.auth.entity.User;
import com.homely.rental.catalog.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "viewings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Viewing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewing_slot_id", nullable = false)
    private ViewingSlot viewingSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ViewingStatus status = ViewingStatus.REQUESTED;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "cancelled_reason", length = 1000)
    private String cancelledReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private int version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

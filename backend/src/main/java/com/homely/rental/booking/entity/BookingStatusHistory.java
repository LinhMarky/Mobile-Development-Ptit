package com.homely.rental.booking.entity;

import com.homely.rental.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "booking_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30, nullable = false)
    private String toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 20, nullable = false)
    private ActorType actorType;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "trace_id", length = 64, nullable = false)
    private String traceId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}

package com.homely.rental.common.audit;

import com.homely.rental.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_type", length = 20, nullable = false)
    private String actorType;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "detail", columnDefinition = "JSON")
    private String detail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}

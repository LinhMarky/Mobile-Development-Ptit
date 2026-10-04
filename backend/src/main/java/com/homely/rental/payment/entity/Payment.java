package com.homely.rental.payment.entity;

import com.homely.rental.auth.entity.User;
import com.homely.rental.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private PaymentProvider provider = PaymentProvider.MOCK_SANDBOX;

    @Column(name = "provider_payment_id", length = 100, unique = true)
    private String providerPaymentId;

    @Column(name = "amount_vnd", precision = 18, scale = 0, nullable = false)
    private BigDecimal amountVnd;

    @Column(name = "currency", columnDefinition = "CHAR(3)", nullable = false)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "succeeded_at")
    private Instant succeededAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "is_test", nullable = false)
    private boolean isTest = true;

    @Version
    private int version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

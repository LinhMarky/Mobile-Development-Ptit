package com.homely.rental.payment.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="payment_webhook_receipts", uniqueConstraints=@UniqueConstraint(columnNames={"provider","event_id"}))
@Getter @Setter @NoArgsConstructor
public class WebhookReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=20) private String provider;
    @Column(name="event_id", nullable=false, columnDefinition="CHAR(36)") private String eventId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id") private Payment payment;
    @Column(name="payload_hash", nullable=false, columnDefinition="BINARY(32)") private byte[] payloadHash;
    @Column(length=30) private String outcome;
    @Column(name="processed_at", nullable=false) private Instant processedAt = Instant.now();
    @Column(name="created_at") private Instant createdAt = Instant.now();
}

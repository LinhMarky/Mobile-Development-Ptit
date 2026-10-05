package com.homely.rental.notification.push;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="push_deliveries",uniqueConstraints=@UniqueConstraint(columnNames={"notification_id","device_id"}))
@Getter @Setter @NoArgsConstructor
public class PushDelivery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="notification_id",nullable=false) private Long notificationId;
    @Column(name="device_id",nullable=false) private Long deviceId;
    @Column(nullable=false,length=20) private String status="PENDING";
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt=Instant.now();
    @Column(name="last_error",length=100) private String lastError;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    @Version private int version;
}

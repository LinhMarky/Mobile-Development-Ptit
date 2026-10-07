package com.homely.rental.notification.push;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.repository.DeviceTokenRepository;
import com.homely.rental.auth.repository.NotificationPreferenceRepository;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Claim in a short transaction, send over the network, then record the outcome. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "homely.fcm.enabled", havingValue = "true")
public class PushWorker {
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 8;
    private static final long LEASE_SECONDS = 180;
    private static final long MAX_AGE_SECONDS = 24 * 60 * 60;
    private static final long FIRST_RETRY_SECONDS = 60;
    private static final long MAX_RETRY_SECONDS = 3600;

    private final PushDeliveryRepository deliveries;
    private final DeviceTokenRepository devices;
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final PushGateway gateway;
    private final PlatformTransactionManager manager;
    private final ObjectProvider<MeterRegistry> meters;

    record Attempt(Long id, int number, Long deviceId, String token, Map<String, String> data) { }

    @Scheduled(fixedDelayString = "${homely.fcm.poll-ms:5000}")
    public void tick() {
        for (Long id : deliveries.due(Instant.now(), PageRequest.of(0, BATCH_SIZE))) {
            Attempt attempt = new TransactionTemplate(manager).execute(tx -> claim(id));
            if (attempt == null) continue;
            PushGateway.PushFailure failure = send(attempt);
            String outcome = new TransactionTemplate(manager).execute(tx -> finish(attempt, failure));
            MeterRegistry registry = meters.getIfAvailable();
            if (registry != null && outcome != null) {
                registry.counter("homely.push.attempts", "outcome", outcome).increment();
            }
        }
    }

    private PushGateway.PushFailure send(Attempt attempt) {
        try {
            gateway.send(attempt.token(), attempt.data());
            return null;
        } catch (PushGateway.PushFailure failure) {
            return failure;
        } catch (RuntimeException failure) {
            return new PushGateway.PushFailure("TRANSPORT_ERROR", true, false);
        }
    }

    private Attempt claim(Long id) {
        var delivery = deliveries.lock(id).orElse(null);
        Instant now = Instant.now();
        if (delivery == null || !Set.of("PENDING", "PROCESSING").contains(delivery.getStatus())
                || delivery.getNextAttemptAt().isAfter(now)) return null;

        if (delivery.getAttempts() >= MAX_ATTEMPTS
                || delivery.getCreatedAt().isBefore(now.minusSeconds(MAX_AGE_SECONDS))) {
            delivery.setStatus("FAILED");
            delivery.setLastError("DELIVERY_EXPIRED");
            return null;
        }

        var notification = notifications.findById(delivery.getNotificationId()).orElse(null);
        var device = devices.findById(delivery.getDeviceId()).orElse(null);
        if (notification == null || device == null || !device.isActive()
                || !device.getUser().getId().equals(notification.getUser().getId())
                || notification.getUser().getStatus() != UserStatus.ACTIVE
                || notification.getUser().isSuspended()) {
            delivery.setStatus("SKIPPED");
            return null;
        }

        var preference = preferences.findByUserId(notification.getUser().getId()).orElse(null);
        boolean chat = notification.getType() == NotificationType.NEW_MESSAGE;
        if (preference != null && !(chat ? preference.isChatPush() : preference.isTransactionPush())) {
            delivery.setStatus("SKIPPED");
            return null;
        }

        delivery.setStatus("PROCESSING");
        delivery.setAttempts(delivery.getAttempts() + 1);
        // A crashed worker leaves a lease that another poll can reclaim.
        delivery.setNextAttemptAt(now.plusSeconds(LEASE_SECONDS));
        Map<String, String> data = Map.of(
                "notification_id", notification.getId().toString(),
                "recipient_id", notification.getUser().getId().toString(),
                "title", notification.getTitle(),
                "body", notification.getBody() == null ? "" : notification.getBody(),
                "ref_type", notification.getRefType() == null ? "" : notification.getRefType(),
                "ref_id", notification.getRefId() == null ? "" : notification.getRefId().toString());
        return new Attempt(id, delivery.getAttempts(), device.getId(), device.getToken(), data);
    }

    private String finish(Attempt attempt, PushGateway.PushFailure failure) {
        var delivery = deliveries.lock(attempt.id()).orElseThrow();
        // Ignore an old response if another worker has already reclaimed the lease.
        if (!"PROCESSING".equals(delivery.getStatus()) || delivery.getAttempts() != attempt.number()) return null;

        if (failure == null) {
            delivery.setStatus("SENT");
            delivery.setLastError(null);
            return "sent";
        }
        delivery.setLastError(failure.code);
        if (failure.invalidToken) {
            devices.findById(attempt.deviceId()).ifPresent(device -> {
                // Do not deactivate a newer token registered while this attempt was in flight.
                if (device.getToken().equals(attempt.token())) device.setActive(false);
            });
        }
        if (!failure.retryable || delivery.getAttempts() >= MAX_ATTEMPTS) {
            delivery.setStatus("FAILED");
        } else {
            delivery.setStatus("PENDING");
            long backoff = Math.min(MAX_RETRY_SECONDS,
                    FIRST_RETRY_SECONDS << Math.min(6, delivery.getAttempts() - 1));
            long jitter = ThreadLocalRandom.current().nextLong(30);
            delivery.setNextAttemptAt(Instant.now().plusSeconds(backoff + jitter));
        }
        return "FAILED".equals(delivery.getStatus()) ? "failed" : "retry";
    }
}

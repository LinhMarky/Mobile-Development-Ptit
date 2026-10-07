package com.homely.rental.common.retention;

import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.repository.*;
import com.homely.rental.common.idempotency.*;
import com.homely.rental.notification.push.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"}, showSql=false)
@ContextConfiguration(classes=RetentionIntegrationTest.Config.class)
class RetentionIntegrationTest {
    @Configuration
    @EntityScan("com.homely.rental")
    @EnableJpaRepositories("com.homely.rental")
    @Import({RetentionService.class, RetentionProperties.class})
    static class Config { }

    @Autowired RetentionService retention;
    @Autowired RetentionProperties properties;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository sessions;
    @Autowired OneTimeTokenRepository tokens;
    @Autowired IdempotencyKeyRepository idempotency;
    @Autowired PushDeliveryRepository deliveries;
    @Autowired DeviceTokenRepository devices;
    @Autowired com.homely.rental.notification.repository.NotificationRepository notifications;
    @Autowired jakarta.persistence.EntityManager entities;

    @Test void removesOnlyExpiredAndTerminalData() {
        User user = new User(); user.setEmail(UUID.randomUUID()+"@test.local"); user.setPassword("hash"); user.setFullName("Retention test"); users.save(user);
        Long oldSession = session(user, Instant.now().minusSeconds(60));
        Long liveSession = session(user, Instant.now().plusSeconds(3600));
        Long oldToken = token(user, Instant.now().minusSeconds(60));
        Long liveToken = token(user, Instant.now().plusSeconds(3600));
        Long completed = key(IdempotencyKeyEntity.IdempotencyStatus.COMPLETED, Instant.now().minusSeconds(60));
        Long processing = key(IdempotencyKeyEntity.IdempotencyStatus.PROCESSING, Instant.now().minusSeconds(60));
        Long liveKey = key(IdempotencyKeyEntity.IdempotencyStatus.COMPLETED, Instant.now().plusSeconds(3600));
        Long oldSent = push("SENT", 40);
        Long oldFailed = push("FAILED", 40);
        Long oldSkipped = push("SKIPPED", 40);
        Long oldPending = push("PENDING", 40);
        Long oldProcessing = push("PROCESSING", 40);
        Long recentSent = push("SENT", 1);
        retention.purgeBatch();
        entities.clear(); // JPQL bulk deletes do not evict already managed entities.
        assertThat(sessions.findById(oldSession)).isEmpty(); assertThat(sessions.findById(liveSession)).isPresent();
        assertThat(tokens.findById(oldToken)).isEmpty(); assertThat(tokens.findById(liveToken)).isPresent();
        assertThat(idempotency.findById(completed)).isEmpty(); assertThat(idempotency.findById(processing)).isPresent(); assertThat(idempotency.findById(liveKey)).isPresent();
        assertThat(deliveries.findById(oldSent)).isEmpty(); assertThat(deliveries.findById(oldFailed)).isEmpty(); assertThat(deliveries.findById(oldSkipped)).isEmpty();
        assertThat(deliveries.findById(oldPending)).isPresent(); assertThat(deliveries.findById(oldProcessing)).isPresent(); assertThat(deliveries.findById(recentSent)).isPresent();
    }

    @Test void eachCleanupBatchIsBounded() {
        properties.setBatchSize(1);
        try {
            push("SENT", 40); push("SENT", 40);
            assertThat(retention.purgeBatch().pushes()).isEqualTo(1);
            assertThat(retention.purgeBatch().pushes()).isEqualTo(1);
            assertThat(retention.purgeBatch().pushes()).isZero();
        } finally { properties.setBatchSize(200); }
    }

    private Long session(User user, Instant expiry) {
        var session = new RefreshToken(); session.setUser(user); session.setTokenHash(UUID.randomUUID().toString());
        session.setInstallationId(UUID.randomUUID().toString()); session.setDeviceName("Test"); session.setExpiresAt(expiry);
        return sessions.saveAndFlush(session).getId();
    }
    private Long token(User user, Instant expiry) {
        var token = new OneTimeToken(); token.setUser(user); token.setTokenHash(UUID.randomUUID().toString());
        token.setPurpose(OneTimeToken.OneTimeTokenPurpose.EMAIL_VERIFICATION); token.setExpiresAt(expiry);
        return tokens.saveAndFlush(token).getId();
    }
    private Long key(IdempotencyKeyEntity.IdempotencyStatus status, Instant expiry) {
        var key = new IdempotencyKeyEntity(); key.setIdempotencyKey(UUID.randomUUID().toString()); key.setStatus(status); key.setExpiresAt(expiry);
        return idempotency.saveAndFlush(key).getId();
    }
    private Long push(String status, int ageDays) {
        User user = new User(); user.setEmail(UUID.randomUUID()+"@test.local"); user.setPassword("hash"); user.setFullName("Push retention"); users.saveAndFlush(user);
        var device = new DeviceToken(); device.setUser(user); device.setToken(UUID.randomUUID().toString());
        device.setInstallationId(UUID.randomUUID().toString()); device.setDeviceName("Test"); devices.saveAndFlush(device);
        var notification = new com.homely.rental.notification.entity.Notification(); notification.setUser(user);
        notification.setType(com.homely.rental.notification.entity.NotificationType.NEW_MESSAGE); notification.setTitle("Retention test"); notifications.saveAndFlush(notification);
        var delivery = new PushDelivery(); delivery.setNotificationId(notification.getId()); delivery.setDeviceId(device.getId()); delivery.setStatus(status);
        delivery.setCreatedAt(Instant.now().minusSeconds(ageDays * 86400L));
        return deliveries.saveAndFlush(delivery).getId();
    }
}

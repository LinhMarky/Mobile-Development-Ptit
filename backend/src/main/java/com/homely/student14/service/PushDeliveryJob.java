package com.homely.student14.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.repository.PushRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Reads committed inbox items; network delivery never holds a database transaction open. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "student14.push-enabled", havingValue = "true")
public class PushDeliveryJob {
    private final PushRepository pushRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void deliverPendingNotifications() {
        for (Map<String, Object> candidate : pushRepository.findDueNotifications()) {
            long notificationId = DatabaseRepository.id(candidate.get("id"));
            Delivery delivery = transactionTemplate.execute(status -> claim(notificationId));
            if (delivery != null) {
                deliver(delivery);
            }
        }
    }

    private Delivery claim(long notificationId) {
        Map<String, Object> notification = pushRepository.lockNotification(notificationId);
        String state = notification.get("push_status").toString();
        Object nextAttempt = notification.get("push_next_attempt_at");
        boolean due =
                nextAttempt == null
                        || !Instant.parse(nextAttempt.toString()).isAfter(Instant.now());
        if ((!state.equals("PENDING") && !state.equals("PROCESSING")) || !due) {
            return null;
        }
        int attempts = Integer.parseInt(notification.get("push_attempts").toString());
        if (attempts >= 8) {
            pushRepository.skipNotification(notificationId, "FAILED");
            return null;
        }
        long userId = DatabaseRepository.id(notification.get("user_id"));
        Map<String, Object> device = pushRepository.findCurrentDeviceBinding(userId).orElse(null);
        if (device == null || !notificationsAllowed(notification, device)) {
            pushRepository.skipNotification(notificationId, "SKIPPED");
            return null;
        }

        int attemptNumber = attempts + 1;
        pushRepository.markProcessing(notificationId, attemptNumber);
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("notification_id", Long.toString(notificationId));
        payload.put("recipient_user_id", Long.toString(userId));
        payload.put("installation_id", device.get("fcm_installation_id").toString());
        payload.put("title", shortened(notification.get("title"), 100));
        payload.put("body", shortened(notification.get("body"), 450));
        payload.put("ref_type", shortened(notification.get("ref_type"), 50));
        payload.put("ref_id", Objects.toString(notification.get("ref_id"), ""));
        return new Delivery(
                notificationId,
                attemptNumber,
                userId,
                device.get("fcm_token").toString(),
                device.get("fcm_token_hash").toString(),
                device.get("fcm_installation_id").toString(),
                payload);
    }

    private boolean notificationsAllowed(
            Map<String, Object> notification, Map<String, Object> device) {
        String type = notification.get("notification_type").toString();
        String preference = "transaction_push";
        if (type.equals("MESSAGE")) {
            preference = "chat_push";
        } else if (type.equals("RECOMMENDATION")) {
            preference = "recommendation_push";
        }
        return DatabaseRepository.flag(device.get(preference));
    }

    private void deliver(Delivery delivery) {
        Map<String, Object> currentDevice =
                pushRepository.findCurrentDeviceBinding(delivery.userId).orElse(null);
        if (currentDevice == null
                || !delivery.tokenHash.equals(currentDevice.get("fcm_token_hash"))
                || !delivery.installationId.equals(currentDevice.get("fcm_installation_id"))) {
            pushRepository.finishAttempt(
                    delivery.notificationId, delivery.attemptNumber, "SKIPPED", 0);
            return;
        }
        Message message =
                Message.builder()
                        .setToken(delivery.token)
                        .putAllData(delivery.payload)
                        .setAndroidConfig(
                                AndroidConfig.builder()
                                        .setPriority(AndroidConfig.Priority.HIGH)
                                        .setTtl(600000)
                                        .build())
                        .build();
        try {
            firebaseMessaging.send(message);
            pushRepository.markSent(delivery.notificationId, delivery.attemptNumber);
        } catch (FirebaseMessagingException exception) {
            if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || exception.getMessagingErrorCode() == MessagingErrorCode.SENDER_ID_MISMATCH) {
                pushRepository.clearInvalidDeviceToken(
                        delivery.userId, delivery.installationId, delivery.tokenHash);
                pushRepository.finishAttempt(
                        delivery.notificationId, delivery.attemptNumber, "SKIPPED", 0);
            } else {
                retry(delivery);
            }
        } catch (RuntimeException exception) {
            retry(delivery);
        }
    }

    private void retry(Delivery delivery) {
        String status = delivery.attemptNumber >= 8 ? "FAILED" : "PENDING";
        long delay = Math.min(3600, 15L * (1L << delivery.attemptNumber));
        pushRepository.finishAttempt(
                delivery.notificationId, delivery.attemptNumber, status, delay);
    }

    private String shortened(Object value, int maximumCodePoints) {
        String text = Objects.toString(value, "");
        int length = text.codePointCount(0, text.length());
        return length <= maximumCodePoints
                ? text
                : text.substring(0, text.offsetByCodePoints(0, maximumCodePoints));
    }

    @RequiredArgsConstructor
    private static class Delivery {
        private final long notificationId;
        private final int attemptNumber;
        private final long userId;
        private final String token;
        private final String tokenHash;
        private final String installationId;
        private final Map<String, String> payload;
    }
}

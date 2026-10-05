package com.homely.rental.booking.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.booking.entity.*;
import com.homely.rental.booking.repository.BookingStatusHistoryRepository;
import com.homely.rental.notification.entity.NotificationType;
import com.homely.rental.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingTransitions {
    private final BookingStatusHistoryRepository history;
    private final NotificationService notifications;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Booking booking, BookingStatus from, BookingStatus to,
                       User actor, ActorType actorType, String reason) {
        BookingStatusHistory item = new BookingStatusHistory();
        item.setBooking(booking);
        item.setFromStatus(from == null ? null : from.name());
        item.setToStatus(to.name());
        item.setActor(actor);
        item.setActorType(actorType);
        item.setReason(reason);
        item.setTraceId(UUID.randomUUID().toString());
        history.save(item);
        if (from == to) return;
        NotificationType type = switch (to) {
            case PENDING -> NotificationType.BOOKING_CREATED;
            case APPROVED -> NotificationType.BOOKING_APPROVED;
            case CONFIRMED -> NotificationType.BOOKING_CONFIRMED;
            case COMPLETED -> NotificationType.BOOKING_COMPLETED;
            case CANCELLED -> NotificationType.BOOKING_CANCELLED;
            case EXPIRED -> NotificationType.BOOKING_EXPIRED;
        };
        String body = "Yêu cầu thuê #" + booking.getId() + ": " + to.name();
        notifications.createNotification(booking.getTenant(), type, "Cập nhật yêu cầu thuê", body, "booking", booking.getId());
        notifications.createNotification(booking.getHost(), type, "Cập nhật yêu cầu thuê", body, "booking", booking.getId());
    }
}

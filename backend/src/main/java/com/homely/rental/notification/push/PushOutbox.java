package com.homely.rental.notification.push;
import com.homely.rental.notification.service.NotificationCreated;
import com.homely.rental.notification.repository.NotificationRepository;
import com.homely.rental.auth.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class PushOutbox {
    private final NotificationRepository notifications;
    private final DeviceTokenRepository devices;
    private final PushDeliveryRepository deliveries;
    @EventListener @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(NotificationCreated event) {
        var notification=notifications.findById(event.notificationId()).orElseThrow();
        for(var device: devices.findByUserIdAndActiveTrue(notification.getUser().getId())) {
            var delivery=new PushDelivery();
            delivery.setNotificationId(notification.getId()); delivery.setDeviceId(device.getId());
            deliveries.save(delivery);
        }
    }
}

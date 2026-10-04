package com.homely.rental.notification.service;
import com.homely.rental.chat.service.ChatMessageCreatedEvent;
import com.homely.rental.auth.repository.UserRepository;
import com.homely.rental.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component @RequiredArgsConstructor
public class ChatPushListener {
    private final UserRepository users;
    private final NotificationService notifications;
    @EventListener @Transactional(propagation=Propagation.MANDATORY)
    public void sent(ChatMessageCreatedEvent event) {
        var tenant=users.findByEmail(event.tenantEmail());
        var host=users.findByEmail(event.hostEmail());
        var recipient=tenant.getId().equals(event.message().getSenderId())?host:tenant;
        notifications.createNotification(recipient,NotificationType.NEW_MESSAGE,"Tin nhắn mới",
                "Bạn có một tin nhắn mới về phòng trọ.","conversation",event.message().getConversationId());
    }
}

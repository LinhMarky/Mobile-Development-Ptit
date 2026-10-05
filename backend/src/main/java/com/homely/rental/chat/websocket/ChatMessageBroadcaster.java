package com.homely.rental.chat.websocket;

import com.homely.rental.chat.service.ChatMessageCreatedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChatMessageBroadcaster {
    private final SimpMessagingTemplate messaging;

    public ChatMessageBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void committed(ChatMessageCreatedEvent event) {
        deliver(event.tenantEmail(), event);
        deliver(event.hostEmail(), event);
    }

    private void deliver(String recipient, ChatMessageCreatedEvent event) {
        try {
            messaging.convertAndSendToUser(recipient, "/queue/chat.messages", event.message());
        } catch (RuntimeException unavailable) {
            // Persistence already committed. A transport failure must not turn its ACK into an error.
            // Clients recover missed realtime deliveries through the REST history cursor.
            log.warn("Chat delivery failed for stored message {}", event.message().getId());
        }
    }
}

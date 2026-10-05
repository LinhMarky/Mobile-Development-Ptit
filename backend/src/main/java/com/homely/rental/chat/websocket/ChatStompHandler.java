package com.homely.rental.chat.websocket;

import com.homely.rental.chat.dto.MessageDTO;
import com.homely.rental.chat.dto.MessageSendFrame;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.dto.ProblemDTO;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatStompHandler {
    private final ChatService chatService;

    public ChatStompHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    @SendToUser(value = "/queue/chat.acks", broadcast = false)
    public MessageDTO send(@Valid @Payload MessageSendFrame frame, Principal principal) {
        // The transactional service returns only after commit; a rollback cannot produce an ACK.
        return chatService.sendMessage(frame, principal.getName());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(value = "/queue/chat.errors", broadcast = false)
    public ProblemDTO error(Exception exception) {
        return ChatProblems.from(exception);
    }
}

package com.homely.rental.chat.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
public class ChatStompErrorHandler extends StompSubProtocolErrorHandler {
    private final ObjectMapper mapper;

    public ChatStompErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable error) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
        headers.setMessage("Chat operation failed");
        headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(ChatProblems.from(error));
        } catch (JsonProcessingException serializationFailure) {
            body = "{\"status\":500,\"code\":\"INTERNAL_ERROR\",\"detail\":\"Chat operation failed\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        return MessageBuilder.createMessage(body, headers.getMessageHeaders());
    }
}

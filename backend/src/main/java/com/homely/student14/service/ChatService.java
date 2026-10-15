package com.homely.student14.service;

import com.homely.student14.dto.request.chat.CreateConversationDTO;
import com.homely.student14.dto.request.chat.SendMessageDTO;
import com.homely.student14.repository.ChatRepository;
import com.homely.student14.repository.DatabaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int MAX_MESSAGE_LENGTH = 3000;

    private final ChatRepository chatRepository;
    private final AuthService authService;

    public List<Map<String, Object>> findConversations(Jwt jwt) {
        Map<String, Object> currentUser = authService.user(jwt);
        long userId = DatabaseRepository.id(currentUser.get("id"));
        return chatRepository.findConversationsByParticipant(userId);
    }

    public Map<String, Object> findConversation(long conversationId, Jwt jwt) {
        Map<String, Object> currentUser = authService.user(jwt);
        long userId = DatabaseRepository.id(currentUser.get("id"));
        Map<String, Object> conversation =
                chatRepository.findConversationById(conversationId, userId);
        checkParticipant(conversation, userId);
        return conversation;
    }

    @Transactional
    public Map<String, Object> createConversation(Jwt jwt, CreateConversationDTO request) {
        Map<String, Object> tenant = authService.role(jwt, "tenant");
        DatabaseRepository.require(
                DatabaseRepository.flag(tenant.get("email_verified")),
                403,
                "Cần xác minh email để nhắn tin.");

        Map<String, Object> room = chatRepository.findPublishedRoom(request.getRoomId());
        long tenantId = DatabaseRepository.id(tenant.get("id"));
        long hostId = DatabaseRepository.id(room.get("host_id"));
        DatabaseRepository.require(tenantId != hostId, 400, "Không thể nhắn tin cho chính mình.");

        long roomId = DatabaseRepository.id(room.get("id"));
        chatRepository.createConversationIfAbsent(
                roomId, tenantId, hostId, room.get("title").toString());
        long conversationId = chatRepository.findConversationId(roomId, tenantId);
        return findConversation(conversationId, jwt);
    }

    public List<Map<String, Object>> findMessages(long conversationId, Jwt jwt) {
        findConversation(conversationId, jwt);
        List<Map<String, Object>> messages = chatRepository.findLatestMessages(conversationId);
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public Map<String, Object> sendMessage(long conversationId, Jwt jwt, SendMessageDTO request) {
        Map<String, Object> currentUser = authService.user(jwt);
        DatabaseRepository.require(
                DatabaseRepository.flag(currentUser.get("email_verified")),
                403,
                "Cần xác minh email để gửi tin.");

        long senderId = DatabaseRepository.id(currentUser.get("id"));
        Map<String, Object> conversation = chatRepository.lockConversation(conversationId);
        checkParticipant(conversation, senderId);
        String content = validateContent(request.getContent());
        String clientMessageId = validateClientMessageId(request.getClientMessageId());

        Optional<Map<String, Object>> existingMessage =
                chatRepository.findMessageByClientId(conversationId, clientMessageId);
        if (existingMessage.isPresent()) {
            Map<String, Object> savedMessage = existingMessage.get();
            DatabaseRepository.require(
                    DatabaseRepository.id(savedMessage.get("sender_id")) == senderId
                            && content.equals(savedMessage.get("content")),
                    409,
                    "Mã gửi trùng nhưng nội dung khác.");
            return savedMessage;
        }

        long nextSequence = Long.parseLong(conversation.get("sequence").toString()) + 1;
        long messageId =
                chatRepository.createMessage(
                        conversationId, senderId, content, clientMessageId, nextSequence);
        chatRepository.updateLastMessage(conversationId, nextSequence, content);

        boolean senderIsHost = senderId == DatabaseRepository.id(conversation.get("host_id"));
        long recipientId =
                DatabaseRepository.id(conversation.get(senderIsHost ? "tenant_id" : "host_id"));
        chatRepository.notifyNewMessage(
                recipientId, currentUser.get("full_name").toString(), content, conversationId);
        return chatRepository.findMessageById(messageId);
    }

    private void checkParticipant(Map<String, Object> conversation, long userId) {
        boolean isHost = userId == DatabaseRepository.id(conversation.get("host_id"));
        boolean isTenant = userId == DatabaseRepository.id(conversation.get("tenant_id"));
        DatabaseRepository.require(isHost || isTenant, 403, "Bạn không thuộc hội thoại này.");
    }

    private String validateContent(String content) {
        String normalizedContent = Objects.toString(content, "").trim();
        DatabaseRepository.require(
                !normalizedContent.isEmpty()
                        && normalizedContent.codePointCount(0, normalizedContent.length())
                                <= MAX_MESSAGE_LENGTH,
                400,
                "Trường content không hợp lệ (tối đa 3000 ký tự).");
        return normalizedContent;
    }

    private String validateClientMessageId(String clientMessageId) {
        String normalizedId = Objects.toString(clientMessageId, "").trim();
        DatabaseRepository.require(
                !normalizedId.isEmpty()
                        && normalizedId.codePointCount(0, normalizedId.length()) <= 36,
                400,
                "Trường client_message_id không hợp lệ (tối đa 36 ký tự).");
        try {
            UUID.fromString(normalizedId);
        } catch (IllegalArgumentException exception) {
            throw DatabaseRepository.fail(400, "Mã tin nhắn không hợp lệ.");
        }
        return normalizedId;
    }
}

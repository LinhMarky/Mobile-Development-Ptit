package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRepository {
    private final DatabaseRepository databaseRepository;

    public List<Map<String, Object>> findConversationsByParticipant(long userId) {
        return databaseRepository.all(
                """
                SELECT c.*, IF(c.host_id = ?, tenant.full_name, host.full_name) AS contact_name
                FROM conversations c
                JOIN users tenant ON tenant.id = c.tenant_id
                JOIN users host ON host.id = c.host_id
                WHERE c.host_id = ? OR c.tenant_id = ?
                ORDER BY COALESCE(c.last_message_at, c.created_at) DESC
                LIMIT 100
                """,
                userId,
                userId,
                userId);
    }

    public Map<String, Object> findConversationById(long conversationId, long userId) {
        return databaseRepository.one(
                """
                SELECT c.*, IF(c.host_id = ?, tenant.full_name, host.full_name) AS contact_name
                FROM conversations c
                JOIN users tenant ON tenant.id = c.tenant_id
                JOIN users host ON host.id = c.host_id
                WHERE c.id = ?
                """,
                userId,
                conversationId);
    }

    public Map<String, Object> findPublishedRoom(long roomId) {
        return databaseRepository.one(
                """
                SELECT r.*, l.title
                FROM rooms r
                JOIN listings l ON l.room_id = r.id
                WHERE r.id = ? AND l.status = 'PUBLISHED'
                  AND (l.expires_at IS NULL OR l.expires_at > UTC_TIMESTAMP(3))
                """,
                roomId);
    }

    public void createConversationIfAbsent(
            long roomId, long tenantId, long hostId, String roomTitle) {
        databaseRepository.update(
                """
                INSERT INTO conversations(room_id, tenant_id, host_id, room_title)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE id = id
                """,
                roomId,
                tenantId,
                hostId,
                roomTitle);
    }

    public long findConversationId(long roomId, long tenantId) {
        Map<String, Object> conversation =
                databaseRepository.one(
                        """
                        SELECT id FROM conversations WHERE room_id = ? AND tenant_id = ?
                        """,
                        roomId,
                        tenantId);
        return DatabaseRepository.id(conversation.get("id"));
    }

    public List<Map<String, Object>> findLatestMessages(long conversationId) {
        return databaseRepository.all(
                """
                SELECT * FROM messages WHERE conversation_id = ? ORDER BY sequence DESC LIMIT 200
                """,
                conversationId);
    }

    public Map<String, Object> lockConversation(long conversationId) {
        return databaseRepository.one(
                """
                SELECT * FROM conversations WHERE id = ? FOR UPDATE
                """,
                conversationId);
    }

    public Optional<Map<String, Object>> findMessageByClientId(
            long conversationId, String clientMessageId) {
        List<Map<String, Object>> messages =
                databaseRepository.all(
                        """
                        SELECT * FROM messages WHERE conversation_id = ? AND client_message_id = ?
                        """,
                        conversationId,
                        clientMessageId);
        return messages.stream().findFirst();
    }

    public long createMessage(
            long conversationId,
            long senderId,
            String content,
            String clientMessageId,
            long sequence) {
        return databaseRepository.insert(
                """
INSERT INTO messages(conversation_id, sender_id, content, client_message_id, sequence)
VALUES (?, ?, ?, ?, ?)
""",
                conversationId,
                senderId,
                content,
                clientMessageId,
                sequence);
    }

    public void updateLastMessage(long conversationId, long sequence, String content) {
        String preview = content.substring(0, Math.min(content.length(), 200));
        databaseRepository.update(
                """
UPDATE conversations
SET sequence = ?, last_message_at = UTC_TIMESTAMP(3), last_message_preview = ?, version = version + 1
WHERE id = ?
""",
                sequence,
                preview,
                conversationId);
    }

    public Map<String, Object> findMessageById(long messageId) {
        return databaseRepository.one(
                """
                SELECT * FROM messages WHERE id = ?
                """,
                messageId);
    }

    public void notifyNewMessage(
            long recipientId, String senderName, String content, long conversationId) {
        databaseRepository.notify(
                recipientId, "MESSAGE", senderName, content, "conversation", conversationId);
    }
}

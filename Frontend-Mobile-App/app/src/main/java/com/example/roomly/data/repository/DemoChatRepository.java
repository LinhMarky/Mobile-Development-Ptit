package com.example.roomly.data.repository;

import androidx.annotation.Nullable;

import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lưu hội thoại demo trong bộ nhớ.
 * Mỗi phòng và người thuê có một hội thoại riêng.
 */
public class DemoChatRepository {

    private static final DemoChatRepository INSTANCE =
            new DemoChatRepository();

    private final Map<String, Conversation> conversations =
            new HashMap<>();

    private final Map<String, String> conversationIdsByRoom =
            new HashMap<>();

    private DemoChatRepository() {
    }

    public static DemoChatRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Chỉ trả về hội thoại của tài khoản đang đăng nhập.
     */
    public List<Conversation> getConversations() {
        List<Conversation> result = new ArrayList<>();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            return result;
        }

        for (Conversation conversation : conversations.values()) {
            if (conversation.isParticipant(session.getUserId())) {
                result.add(conversation);
            }
        }

        result.sort((first, second) -> Long.compare(
                second.getLastActivityMillis(),
                first.getLastActivityMillis()
        ));

        return result;
    }

    /**
     * Không cho tài khoản ngoài hội thoại đọc nội dung.
     */
    @Nullable
    public Conversation getConversation(String conversationId) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn() || conversationId == null) {
            return null;
        }

        Conversation conversation = conversations.get(conversationId);

        if (conversation == null
                || !conversation.isParticipant(session.getUserId())) {
            return null;
        }

        return conversation;
    }

    /**
     * Người thuê mở hội thoại với chủ phòng.
     * Chủ trọ trả lời từ danh sách hội thoại của mình.
     */
    public Conversation getOrCreateConversation(RoomCard room) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            throw new IllegalStateException(
                    "Bạn cần đăng nhập để nhắn tin."
            );
        }

        if (!session.hasRole(UserRole.TENANT)) {
            throw new IllegalStateException(
                    "Bạn cần quyền người thuê để bắt đầu hội thoại với chủ trọ."
            );
        }

        if (!session.isEmailVerified()) {
            throw new IllegalStateException(
                    "Bạn cần xác minh email trước khi bắt đầu hội thoại."
            );
        }

        if (room == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy phòng."
            );
        }

        // Với bài đăng công khai, lấy lại dữ liệu mới nhất.
        String listingId = cleanText(room.getListingId());

        if (!listingId.isEmpty()) {
            room = DemoAdminListingRepository.getInstance()
                    .getPublishedRoomCardById(listingId);

            if (room == null) {
                throw new IllegalStateException(
                        "Bài đăng không còn hiển thị."
                );
            }
        }

        String roomId = cleanText(room.getRoomId());
        String hostId = cleanText(room.getOwnerId());
        String tenantId = cleanText(session.getUserId());

        if (roomId.isEmpty() || hostId.isEmpty() || tenantId.isEmpty()) {
            throw new IllegalStateException(
                    "Thiếu mã phòng hoặc thông tin thành viên hội thoại."
            );
        }

        if (hostId.equals(tenantId)) {
            throw new IllegalStateException(
                    "Bạn không thể nhắn tin với chính mình."
            );
        }

        String roomKey = createRoomKey(roomId, tenantId);
        String existingId = conversationIdsByRoom.get(roomKey);

        if (existingId != null) {
            Conversation existing = conversations.get(existingId);

            if (existing != null) {
                if (!hostId.equals(existing.getHostId())
                        || !existing.isParticipant(tenantId)) {
                    throw new IllegalStateException(
                            "Thông tin chủ phòng đã thay đổi."
                    );
                }

                return existing;
            }
        }

        String id = UUID.randomUUID().toString();

        Conversation conversation = new Conversation(
                id,
                roomId,
                hostId,
                tenantId,
                room.getHostName(),
                session.getFullName(),
                room.getTitle(),
                room.getImageResId(),
                System.currentTimeMillis()
        );

        conversations.put(id, conversation);
        conversationIdsByRoom.put(roomKey, id);

        return conversation;
    }

    /**
     * Chỉ thành viên đã xác minh email được gửi tin.
     */
    public boolean sendMessage(
            String conversationId,
            String content
    ) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn() || !session.isEmailVerified()) {
            return false;
        }

        Conversation conversation = getConversation(conversationId);

        if (conversation == null || content == null) {
            return false;
        }

        String trimmedContent = content.trim();

        if (trimmedContent.isEmpty() || trimmedContent.length() > 5000) {
            return false;
        }

        conversation.addMessage(new ChatMessage(
                trimmedContent,
                System.currentTimeMillis(),
                session.getUserId()
        ));

        return true;
    }

    private String createRoomKey(String roomId, String tenantId) {
        return roomId.length() + ":" + roomId
                + tenantId.length() + ":" + tenantId;
    }

    private String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}
package com.example.roomly.data.repository;

import androidx.annotation.Nullable;

import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.model.RoomCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Giữ các cuộc trò chuyện mẫu trong bộ nhớ.
 * Tin nhắn chưa được gửi đến backend hoặc chủ phòng thật.
 */
public class DemoChatRepository {

    private static final DemoChatRepository INSTANCE =
            new DemoChatRepository();

    // Tra cứu hội thoại theo mã riêng.
    private final Map<String, Conversation> conversations =
            new HashMap<>();

    // Ghép phòng mẫu với hội thoại để tránh tạo trùng khi mở lại.
    private final Map<String, String> conversationIdsByRoom =
            new HashMap<>();

    /**
     * Chỉ tạo repository dùng chung bên trong lớp này.
     */
    private DemoChatRepository() {
    }

    /**
     * Trả về repository dùng chung trong ứng dụng.
     */
    public static DemoChatRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Trả về danh sách hội thoại, sắp xếp hoạt động mới nhất lên đầu.
     */
    public List<Conversation> getConversations() {
        List<Conversation> result =
                new ArrayList<>(conversations.values());

        result.sort((first, second) -> Long.compare(
                second.getLastActivityMillis(),
                first.getLastActivityMillis()
        ));

        return result;
    }

    /**
     * Tìm hội thoại theo mã; trả về null nếu không còn dữ liệu.
     */
    @Nullable
    public Conversation getConversation(String conversationId) {
        return conversations.get(conversationId);
    }

    /**
     * Mở lại hội thoại của phòng mẫu hoặc tạo mới nếu chưa có.
     * Tên người liên hệ hiện dùng dữ liệu mẫu.
     */
    public Conversation getOrCreateConversation(RoomCard room) {
        String roomKey = createRoomKey(room);
        String existingId = conversationIdsByRoom.get(roomKey);

        if (existingId != null) {
            Conversation existing = conversations.get(existingId);

            if (existing != null) {
                return existing;
            }
        }

        String id = UUID.randomUUID().toString();

        Conversation conversation = new Conversation(
                id,
                "Nguyễn Minh Anh",
                room.getTitle(),
                room.getImageResId(),
                System.currentTimeMillis()
        );

        conversations.put(id, conversation);
        conversationIdsByRoom.put(roomKey, id);

        return conversation;
    }

    /**
     * Thêm tin nhắn do người dùng gửi vào hội thoại mẫu.
     * Trả về false nếu không tìm thấy hội thoại hoặc nội dung trống.
     */
    public boolean sendMessage(
            String conversationId,
            String content
    ) {
        Conversation conversation =
                conversations.get(conversationId);

        if (conversation == null || content == null) {
            return false;
        }

        String trimmedContent = content.trim();

        if (trimmedContent.isEmpty()) {
            return false;
        }

        conversation.addMessage(new ChatMessage(
                trimmedContent,
                System.currentTimeMillis(),
                true
        ));

        return true;
    }

    /**
     * Tạo khóa từ tên và địa chỉ để nhận diện phòng trong dữ liệu mẫu.
     * Khi nối backend, khóa này cần được thay bằng mã phòng thật.
     */
    private String createRoomKey(RoomCard room) {
        String title = room.getTitle();
        String address = room.getAddress();

        // Ghi độ dài tên để phân biệt các cặp tên và địa chỉ.
        return title.length() + ":" + title + address;
    }
}
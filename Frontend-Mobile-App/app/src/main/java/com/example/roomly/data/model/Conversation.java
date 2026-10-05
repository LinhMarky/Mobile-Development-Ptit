package com.example.roomly.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu một cuộc trò chuyện mẫu liên quan đến một phòng.
 */
public class Conversation {

    private final String id;
    private final String contactName;
    private final String roomTitle;
    private final int roomImageResId;
    private final long createdAtMillis;

    // Danh sách tin nhắn theo thứ tự được thêm.
    private final List<ChatMessage> messages = new ArrayList<>();

    /**
     * Khởi tạo hội thoại với mã riêng, người liên hệ và phòng.
     */
    public Conversation(
            String id,
            String contactName,
            String roomTitle,
            int roomImageResId,
            long createdAtMillis
    ) {
        this.id = id;
        this.contactName = contactName;
        this.roomTitle = roomTitle;
        this.roomImageResId = roomImageResId;
        this.createdAtMillis = createdAtMillis;
    }

    /**
     * Trả về mã hội thoại để mở đúng màn hình trò chuyện.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về tên người liên hệ.
     */
    public String getContactName() {
        return contactName;
    }

    /**
     * Trả về tên phòng liên quan đến hội thoại.
     */
    public String getRoomTitle() {
        return roomTitle;
    }

    /**
     * Trả về mã tài nguyên ảnh phòng.
     */
    public int getRoomImageResId() {
        return roomImageResId;
    }

    /**
     * Trả về bản sao danh sách để bên ngoài không sửa trực tiếp
     * danh sách tin nhắn bên trong hội thoại.
     */
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Thêm một tin nhắn vào cuối cuộc trò chuyện.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
    }

    /**
     * Trả về tin nhắn gần nhất để hiển thị trong danh sách hội thoại.
     */
    public String getLastMessagePreview() {
        if (messages.isEmpty()) {
            return "Chưa có tin nhắn";
        }

        ChatMessage lastMessage = messages.get(messages.size() - 1);

        return lastMessage.isSentByMe()
                ? "Bạn: " + lastMessage.getContent()
                : lastMessage.getContent();
    }

    /**
     * Trả về thời gian tin nhắn cuối, hoặc thời gian tạo
     * nếu hội thoại chưa có tin nhắn.
     */
    public long getLastActivityMillis() {
        if (messages.isEmpty()) {
            return createdAtMillis;
        }

        return messages.get(messages.size() - 1).getSentAtMillis();
    }
}
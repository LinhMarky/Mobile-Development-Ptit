package com.example.roomly.data.model;

import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private final String id;
    private final String roomId;
    private final String hostId;
    private final String tenantId;
    private final String hostName;
    private final String tenantName;
    private final String contactName;
    private final String roomTitle;
    private final int roomImageResId;
    private final long createdAtMillis;

    private final List<ChatMessage> messages = new ArrayList<>();

    // Giữ tương thích với cách tạo hội thoại cũ.
    public Conversation(
            String id,
            String contactName,
            String roomTitle,
            int roomImageResId,
            long createdAtMillis) {
        this.id = id;
        this.roomId = "";
        this.hostId = "";
        this.tenantId = "";
        this.hostName = "";
        this.tenantName = "";
        this.contactName = cleanText(contactName);
        this.roomTitle = cleanText(roomTitle);
        this.roomImageResId = roomImageResId;
        this.createdAtMillis = createdAtMillis;
    }

    // Hội thoại mới có phòng và hai thành viên rõ ràng.
    public Conversation(
            String id,
            String roomId,
            String hostId,
            String tenantId,
            String hostName,
            String tenantName,
            String roomTitle,
            int roomImageResId,
            long createdAtMillis) {
        String cleanRoomId = cleanText(roomId);
        String cleanHostId = cleanText(hostId);
        String cleanTenantId = cleanText(tenantId);

        if (cleanRoomId.isEmpty() || cleanHostId.isEmpty() || cleanTenantId.isEmpty()) {
            throw new IllegalArgumentException("Hội thoại cần có mã phòng, chủ trọ và người thuê.");
        }

        if (cleanHostId.equals(cleanTenantId)) {
            throw new IllegalArgumentException("Bạn không thể tạo hội thoại với chính mình.");
        }

        this.id = id;
        this.roomId = cleanRoomId;
        this.hostId = cleanHostId;
        this.tenantId = cleanTenantId;
        this.hostName = cleanText(hostName);
        this.tenantName = cleanText(tenantName);
        this.contactName = this.hostName.isEmpty() ? "Chủ trọ" : this.hostName;
        this.roomTitle = cleanText(roomTitle);
        this.roomImageResId = roomImageResId;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isParticipant(String userId) {
        String cleanedUserId = cleanText(userId);

        return !cleanedUserId.isEmpty()
                && (cleanedUserId.equals(hostId) || cleanedUserId.equals(tenantId));
    }

    // Giữ để các đoạn giao diện cũ vẫn biên dịch.
    public String getContactName() {
        return contactName;
    }

    public String getContactName(String currentUserId) {
        String userId = cleanText(currentUserId);

        if (!hostId.isEmpty() && hostId.equals(userId)) {
            return tenantName.isEmpty() ? "Người thuê" : tenantName;
        }

        if (!tenantId.isEmpty() && tenantId.equals(userId)) {
            return hostName.isEmpty() ? "Chủ trọ" : hostName;
        }

        return "Hội thoại";
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public int getRoomImageResId() {
        return roomImageResId;
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public void addMessage(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Tin nhắn không được để trống.");
        }

        messages.add(message);
    }

    public String getLastMessagePreview() {
        if (messages.isEmpty()) {
            return "Chưa có tin nhắn";
        }

        return messages.get(messages.size() - 1).getContent();
    }

    public String getLastMessagePreview(String currentUserId) {
        if (messages.isEmpty()) {
            return "Chưa có tin nhắn";
        }

        ChatMessage lastMessage = messages.get(messages.size() - 1);

        return lastMessage.isSentByMe(currentUserId)
                ? "Bạn: " + lastMessage.getContent()
                : lastMessage.getContent();
    }

    public long getLastActivityMillis() {
        if (messages.isEmpty()) {
            return createdAtMillis;
        }

        return messages.get(messages.size() - 1).getSentAtMillis();
    }

    private static String cleanText(String text) {
        return text == null ? "" : text.trim();
    }
}

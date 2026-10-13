package com.example.roomly.data.model;

/**
 * Dữ liệu một tin nhắn.
 * Xác định bên hiển thị bằng ID người gửi và tài khoản đang xem.
 */
public class ChatMessage {

    private final String content;
    private final long sentAtMillis;
    private final String senderId;

    // Giữ tương thích với code demo cũ trong lúc chuyển đổi.
    private final boolean legacySentByMe;

    public ChatMessage(
            String content,
            long sentAtMillis,
            boolean sentByMe
    ) {
        this.content = content == null ? "" : content;
        this.sentAtMillis = sentAtMillis;
        this.senderId = "";
        this.legacySentByMe = sentByMe;
    }

    /**
     * Constructor dùng cho tin nhắn có ID người gửi.
     */
    public ChatMessage(
            String content,
            long sentAtMillis,
            String senderId
    ) {
        if (senderId == null || senderId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tin nhắn cần có mã người gửi."
            );
        }

        this.content = content == null ? "" : content;
        this.sentAtMillis = sentAtMillis;
        this.senderId = senderId.trim();
        this.legacySentByMe = false;
    }

    public String getContent() {
        return content;
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    public String getSenderId() {
        return senderId;
    }

    /**
     * So sánh với tài khoản đang xem cuộc trò chuyện.
     */
    public boolean isSentByMe(String currentUserId) {
        return currentUserId != null
                && !currentUserId.trim().isEmpty()
                && !senderId.isEmpty()
                && senderId.equals(currentUserId.trim());
    }

    /**
     * Chỉ giữ để các đoạn code cũ vẫn biên dịch.
     * Adapter sẽ chuyển sang isSentByMe(currentUserId).
     */
    @Deprecated
    public boolean isSentByMe() {
        return legacySentByMe;
    }
}
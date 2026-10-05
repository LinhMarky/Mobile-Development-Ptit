package com.example.roomly.data.model;

/**
 * Dữ liệu một tin nhắn trong cuộc trò chuyện mẫu.
 */
public class ChatMessage {

    private final String content;
    private final long sentAtMillis;
    private final boolean sentByMe;

    /**
     * Khởi tạo nội dung, thời gian và người gửi tin nhắn.
     * sentByMe là true nếu tin do người dùng hiện tại gửi.
     */
    public ChatMessage(
            String content,
            long sentAtMillis,
            boolean sentByMe
    ) {
        this.content = content;
        this.sentAtMillis = sentAtMillis;
        this.sentByMe = sentByMe;
    }

    /**
     * Trả về nội dung tin nhắn.
     */
    public String getContent() {
        return content;
    }

    /**
     * Trả về thời gian gửi tính bằng mili giây.
     */
    public long getSentAtMillis() {
        return sentAtMillis;
    }

    /**
     * Cho biết tin nhắn có do người dùng hiện tại gửi hay không.
     */
    public boolean isSentByMe() {
        return sentByMe;
    }
}
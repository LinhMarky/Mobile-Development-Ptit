package com.homely.rental.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConversationDTO {
    private Long id;
    private Long roomId;
    private Long tenantId;
    private Long hostId;
    private String roomTitle;
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private long sequence;
    private long lastReadSequence;
    private long unreadCount;
    private Instant createdAt;
}

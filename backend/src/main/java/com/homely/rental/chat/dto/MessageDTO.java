package com.homely.rental.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.homely.rental.chat.entity.MessageContentType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String clientMessageId;
    private long sequence;
    private MessageContentType contentType;
    private Instant createdAt;
}

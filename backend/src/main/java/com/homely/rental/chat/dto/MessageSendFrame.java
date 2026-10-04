package com.homely.rental.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.homely.rental.chat.entity.MessageContentType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageSendFrame {
    @NotNull
    @Positive
    private Long conversationId;

    @NotBlank
    @Size(max = 5000)
    private String content;

    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String clientMessageId;

    @NotNull
    private MessageContentType contentType = MessageContentType.TEXT;
}

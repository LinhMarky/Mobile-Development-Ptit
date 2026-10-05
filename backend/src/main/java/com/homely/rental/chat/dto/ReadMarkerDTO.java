package com.homely.rental.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReadMarkerDTO {
    private Long conversationId;
    private Long userId;
    private long lastReadSequence;
    private Instant updatedAt;
}

package com.homely.rental.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String type;
    private String title;
    private String body;

    @JsonProperty("ref_type")
    private String refType;

    @JsonProperty("ref_id")
    private Long refId;

    @JsonProperty("is_read")
    private boolean isRead;

    @JsonProperty("created_at")
    private Instant createdAt;
}

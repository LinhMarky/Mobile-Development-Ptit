package com.homely.rental.interaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportDTO {
    private Long id;

    @JsonProperty("reporter_id")
    private Long reporterId;

    @JsonProperty("target_type")
    private String targetType;

    @JsonProperty("target_id")
    private Long targetId;

    @JsonProperty("reason_code")
    private String reasonCode;

    private String description;
    private String status;

    @JsonProperty("created_at")
    private Instant createdAt;
}

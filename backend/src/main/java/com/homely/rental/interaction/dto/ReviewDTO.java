package com.homely.rental.interaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewDTO {
    private Long id;

    @JsonProperty("booking_id")
    private Long bookingId;

    @JsonProperty("reviewer_id")
    private Long reviewerId;

    @JsonProperty("reviewer_name")
    private String reviewerName;

    @JsonProperty("room_id")
    private Long roomId;

    private int rating;
    private String comment;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}

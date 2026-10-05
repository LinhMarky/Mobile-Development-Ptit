package com.homely.rental.interaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ViewingDTO {
    private Long id;

    @JsonProperty("viewing_slot_id")
    private Long viewingSlotId;

    @JsonProperty("tenant_id")
    private Long tenantId;

    @JsonProperty("room_id")
    private Long roomId;

    @JsonProperty("host_id")
    private Long hostId;

    private String status;
    private String note;

    @JsonProperty("cancelled_reason")
    private String cancelledReason;

    @JsonProperty("slot_start_at")
    private Instant slotStartAt;

    @JsonProperty("slot_end_at")
    private Instant slotEndAt;

    @JsonProperty("completed_at")
    private Instant completedAt;

    @JsonProperty("created_at")
    private Instant createdAt;
}

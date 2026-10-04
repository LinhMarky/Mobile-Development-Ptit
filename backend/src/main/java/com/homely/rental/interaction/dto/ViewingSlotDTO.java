package com.homely.rental.interaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ViewingSlotDTO {
    private Long id;

    @JsonProperty("room_id")
    private Long roomId;

    @JsonProperty("host_id")
    private Long hostId;

    @JsonProperty("start_at")
    private Instant startAt;

    @JsonProperty("end_at")
    private Instant endAt;

    private String status;
}

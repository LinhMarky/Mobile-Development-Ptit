package com.homely.rental.interaction.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
public class ViewingSlotCreateRequest {

    @NotNull(message = "room_id is required")
    private Long roomId;

    @NotNull(message = "start_at is required")
    @Future(message = "start_at must be in the future")
    private Instant startAt;

    @NotNull(message = "end_at is required")
    @Future(message = "end_at must be in the future")
    private Instant endAt;
}

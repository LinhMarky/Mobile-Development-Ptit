package com.homely.rental.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class BookingCreateRequest {

    @NotNull(message = "room_id is required")
    @JsonProperty("room_id")
    private Long roomId;

    @JsonProperty("desired_move_in")
    @Future(message = "desired_move_in must be a future date")
    private LocalDate desiredMoveIn;

    @JsonProperty("occupant_count")
    @Min(value = 1, message = "occupant_count min is 1")
    @Max(value = 10, message = "occupant_count max is 10")
    private int occupantCount = 1;

    @Size(max = 1000, message = "note must be at most 1000 characters")
    private String note;
}

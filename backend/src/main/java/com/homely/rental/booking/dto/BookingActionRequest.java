package com.homely.rental.booking.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingActionRequest {

    @Size(max = 1000, message = "reason must be at most 1000 characters")
    private String reason;
}

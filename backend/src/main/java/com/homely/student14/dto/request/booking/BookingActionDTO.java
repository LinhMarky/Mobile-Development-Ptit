package com.homely.student14.dto.request.booking;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingActionDTO {

    @Size(max = 1000)
    private String reason;

    private boolean acceptedTerms;
}

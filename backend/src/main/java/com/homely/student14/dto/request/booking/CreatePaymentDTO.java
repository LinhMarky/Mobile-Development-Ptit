package com.homely.student14.dto.request.booking;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentDTO {

    @NotNull @Positive private Long bookingId;
    private boolean acceptedTerms;
}

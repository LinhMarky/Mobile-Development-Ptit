package com.homely.student14.dto.request.booking;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewDTO {

    @NotNull @Positive private Long bookingId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 3000)
    private String comment;
}

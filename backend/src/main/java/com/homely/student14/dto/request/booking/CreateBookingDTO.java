package com.homely.student14.dto.request.booking;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingDTO {

    @NotNull @Positive private Long roomId;

    @Min(1)
    private int occupantCount = 1;

    @NotBlank private String desiredMoveIn;

    @Size(max = 1000)
    private String note;
}

package com.homely.rental.interaction.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewUpdateRequest {

    @Min(value = 1, message = "rating min is 1")
    @Max(value = 5, message = "rating max is 5")
    private Integer rating;

    @Size(max = 2000, message = "comment must be at most 2000 characters")
    private String comment;
}

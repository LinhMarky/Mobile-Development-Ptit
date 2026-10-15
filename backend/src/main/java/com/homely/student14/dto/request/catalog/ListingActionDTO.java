package com.homely.student14.dto.request.catalog;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingActionDTO {
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự.")
    private String reason = "";
}

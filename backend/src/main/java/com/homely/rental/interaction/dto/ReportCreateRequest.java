package com.homely.rental.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportCreateRequest {

    @NotBlank(message = "target_type is required")
    @Size(max = 50)
    private String targetType;

    @NotNull(message = "target_id is required")
    private Long targetId;

    @NotBlank(message = "reason_code is required")
    @Size(max = 50)
    private String reasonCode;

    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;
}

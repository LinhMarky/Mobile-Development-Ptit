package com.homely.rental.interaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ViewingCreateRequest {

    @NotNull(message = "viewing_slot_id is required")
    private Long viewingSlotId;

    @Size(max = 1000, message = "note must be at most 1000 characters")
    private String note;
}

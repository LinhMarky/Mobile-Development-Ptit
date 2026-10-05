package com.homely.rental.media.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request to attach uploaded media to a resource (room, listing, etc).
 */
@Getter
@Setter
public class AttachMediaRequest {

    @NotNull(message = "media_ids is required")
    @Size(min = 1, max = 16, message = "media_ids must have 1-16 items")
    private List<Long> mediaIds;
}

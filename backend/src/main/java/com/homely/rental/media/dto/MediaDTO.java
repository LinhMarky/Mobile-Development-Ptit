package com.homely.rental.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Media DTO returned after upload and in resource listings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDTO {
    private Long id;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("file_size_bytes")
    private long fileSizeBytes;

    @JsonProperty("original_filename")
    private String originalFilename;

    private String url;

    private Integer width;
    private Integer height;

    @JsonProperty("duration_secs")
    private Integer durationSecs;

    private String purpose;
    private String status;

    @JsonProperty("display_order")
    private int displayOrder;

    @JsonProperty("created_at")
    private Instant createdAt;
}

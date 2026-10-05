package com.homely.rental.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingDTO {
    private Long id;

    @JsonProperty("tenant_id")
    private Long tenantId;

    @JsonProperty("host_id")
    private Long hostId;

    @JsonProperty("room_id")
    private Long roomId;

    @JsonProperty("listing_id")
    private Long listingId;

    private String status;

    @JsonProperty("desired_move_in")
    private LocalDate desiredMoveIn;

    @JsonProperty("occupant_count")
    private int occupantCount;

    @JsonProperty("rent_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private BigDecimal rentVnd;

    @JsonProperty("deposit_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private BigDecimal depositVnd;

    private String note;

    @JsonProperty("request_expires_at")
    private Instant requestExpiresAt;

    @JsonProperty("hold_expires_at")
    private Instant holdExpiresAt;

    @JsonProperty("handover_due_at")
    private Instant handoverDueAt;

    @JsonProperty("completed_at")
    private Instant completedAt;

    @JsonProperty("cancelled_at")
    private Instant cancelledAt;

    @JsonProperty("last_reason")
    private String lastReason;

    private int version;

    @JsonProperty("created_at")
    private Instant createdAt;
}

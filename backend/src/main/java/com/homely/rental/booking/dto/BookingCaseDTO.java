package com.homely.rental.booking.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homely.rental.booking.entity.BookingCase;
import java.math.BigDecimal;
import java.time.Instant;
public record BookingCaseDTO(Long id, @JsonProperty("booking_id") Long bookingId, String type, String status,
        String description, String decision, @JsonProperty("resolution_note") String resolutionNote,
        @JsonProperty("tenant_refund_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal tenantRefundVnd,
        @JsonProperty("host_retained_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal hostRetainedVnd,
        @JsonProperty("created_at") Instant createdAt) {
    public static BookingCaseDTO of(BookingCase c) {
        return new BookingCaseDTO(c.getId(),c.getBooking().getId(),c.getType().name(),c.getStatus().name(),
                c.getDescription(),c.getDecision()==null?null:c.getDecision().name(),c.getResolutionNote(),
                c.getTenantRefundVnd(),c.getHostRetainedVnd(),c.getCreatedAt());
    }
}

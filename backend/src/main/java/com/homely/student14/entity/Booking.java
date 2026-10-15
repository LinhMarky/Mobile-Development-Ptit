package com.homely.student14.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Database record used by the booking service. SQL is kept in BookingRepository. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Booking {

    private Long id;
    private Long tenantId;
    private Long hostId;
    private Long roomId;
    private Long listingId;
    private String status;
    private Integer occupantCount;
    private BigDecimal rentVnd;
    private BigDecimal depositVnd;
    private Long allocatedPaymentId;
    private Instant requestExpiresAt;
    private Instant holdExpiresAt;
    private Instant tenantTermsAcceptedAt;
    private Instant hostHandoverConfirmedAt;
    private Instant tenantHandoverConfirmedAt;
    private LocalDate desiredMoveIn;
    private String note;
}

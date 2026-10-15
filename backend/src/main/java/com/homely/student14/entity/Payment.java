package com.homely.student14.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Database record used by the booking service. SQL is kept in BookingRepository. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {

    private Long id;
    private Long bookingId;
    private Long payerId;
    private BigDecimal amountVnd;
    private String status;
    private String provider;
    private String refundStatus;

    @JsonProperty("is_test")
    private boolean test;

    private Instant expiresAt;
}

package com.homely.rental.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentDTO {
    private Long id;

    @JsonProperty("booking_id")
    private Long bookingId;

    @JsonProperty("payer_id")
    private Long payerId;

    private String provider;

    @JsonProperty("provider_payment_id")
    private String providerPaymentId;

    @JsonProperty("amount_vnd") @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private BigDecimal amountVnd;

    private String status;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    @JsonProperty("succeeded_at")
    private Instant succeededAt;

    @JsonProperty("is_test")
    private boolean isTest;

    @JsonProperty("created_at")
    private Instant createdAt;
}

package com.homely.rental.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WebhookPayload {

    @NotBlank
    @JsonProperty("provider_payment_id")
    private String providerPaymentId;

    @NotBlank
    @JsonProperty("event_id")
    private String eventId;

    @NotBlank
    private String status; // "SUCCEEDED", "FAILED"

    @JsonProperty("failure_code")
    private String failureCode;
    
    // In a real system, there would be a signature for verification
    private String signature;
}

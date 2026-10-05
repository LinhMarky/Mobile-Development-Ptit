package com.homely.rental.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentCreateRequest {

    @NotNull(message = "booking_id is required")
    private Long bookingId;
    
    // In a real system, we might pass redirect_url or other provider specific info here
}

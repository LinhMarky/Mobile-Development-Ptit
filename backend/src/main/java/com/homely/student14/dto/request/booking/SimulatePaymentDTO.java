package com.homely.student14.dto.request.booking;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulatePaymentDTO {

    @NotBlank private String status;
}

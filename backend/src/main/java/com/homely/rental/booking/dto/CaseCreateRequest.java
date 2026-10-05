package com.homely.rental.booking.dto;
import com.homely.rental.booking.entity.BookingCaseType;
import jakarta.validation.constraints.*;
public record CaseCreateRequest(@NotNull BookingCaseType type, @NotBlank @Size(max=2000) String description) {}

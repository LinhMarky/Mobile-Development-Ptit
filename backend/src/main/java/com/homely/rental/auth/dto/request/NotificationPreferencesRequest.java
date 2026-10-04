package com.homely.rental.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Notification preferences update request (AUTH11).
 */
@Getter
@Setter
public class NotificationPreferencesRequest {
    @NotNull(message = "expected_version is required")
    private Integer expectedVersion;

    private Boolean transactionPush;
    private Boolean transactionEmail;
    private Boolean chatPush;
    private Boolean recommendationPush;
}

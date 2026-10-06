package com.homely.rental.auth.dto.response;

import com.homely.rental.auth.entity.AccountDeletionRequest;
import java.time.Instant;

public record AccountDeletionResponse(Long id, String status, String failureCode,
                                       Instant createdAt, Instant completedAt) {
    public static AccountDeletionResponse from(AccountDeletionRequest request) {
        return new AccountDeletionResponse(request.getId(), request.getStatus().name(),
                request.getFailureCode(), request.getCreatedAt(), request.getCompletedAt());
    }
}

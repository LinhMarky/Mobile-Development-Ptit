package com.homely.rental.auth.service;

import com.homely.rental.auth.dto.request.DeletionRequest;
import com.homely.rental.auth.entity.AccountDeletionRequest;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.AccountDeletionRequestRepository;
import com.homely.rental.auth.security.AccountAccessService;
import com.homely.rental.auth.security.SecurityUtils;
import com.homely.rental.auth.dto.response.AccountDeletionResponse;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Account deletion service (AUTH12, §2.8).
 * Validates no blocking resources exist before accepting deletion request.
 */
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final AccountAccessService accounts;
    private final PasswordEncoder passwordEncoder;
    private final AccountDeletionEligibility eligibility;

    @Transactional
    public AccountDeletionRequest requestDeletion(DeletionRequest dto) throws IdInvalidException {
        User user = accounts.requireActiveIgnoringDeletion(SecurityUtils.getCurrentUserLogin().orElse(null));

        // Verify current password
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IdInvalidException("Current password is incorrect");
        }

        // Check for existing pending deletion request
        if (deletionRequestRepository.existsByUserIdAndStatusIn(user.getId(),
                List.of(AccountDeletionRequest.DeletionStatus.PENDING, AccountDeletionRequest.DeletionStatus.PROCESSING))) {
            throw new ConflictException("DELETION_ALREADY_REQUESTED",
                    "An account deletion request is already pending");
        }

        eligibility.requireEligible(user.getId());

        // The schema permits one request per user. Reuse a FAILED request on retry.
        AccountDeletionRequest request = deletionRequestRepository.findByUserId(user.getId())
                .orElseGet(AccountDeletionRequest::new);
        request.setUser(user);
        request.setReason(dto.getReason() != null ? dto.getReason() : "");
        request.setStatus(AccountDeletionRequest.DeletionStatus.PENDING);
        request.setFailureCode(null);
        request.setCompletedAt(null);
        request.setUpdatedAt(java.time.Instant.now());

        return deletionRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public AccountDeletionResponse currentRequest() {
        User user = accounts.requireActiveIgnoringDeletion(SecurityUtils.getCurrentUserLogin().orElse(null));
        return deletionRequestRepository.findByUserId(user.getId()).map(AccountDeletionResponse::from)
                .orElseThrow(() -> new com.homely.rental.common.exception.ResourceNotFoundException("AccountDeletionRequest", user.getId()));
    }
}

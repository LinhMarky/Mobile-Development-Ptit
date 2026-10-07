package com.homely.rental.auth.service;

import com.homely.rental.auth.constant.UserStatus;
import com.homely.rental.auth.entity.AccountDeletionRequest;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.*;
import com.homely.rental.catalog.entity.ListingStatus;
import com.homely.rental.catalog.repository.ListingRepository;
import com.homely.rental.catalog.repository.RoomRepository;
import com.homely.rental.catalog.repository.WishlistRepository;
import com.homely.rental.common.exception.ConflictException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** One request per transaction: either every change commits or none does. */
@Service
@RequiredArgsConstructor
public class AccountDeletionProcessor {
    private final AccountDeletionRequestRepository requests;
    private final UserRepository users;
    private final RoomRepository rooms;
    private final ListingRepository listings;
    private final WishlistRepository wishlist;
    private final RefreshTokenRepository sessions;
    private final DeviceTokenRepository devices;
    private final OneTimeTokenRepository verificationTokens;
    private final AccountDeletionEligibility eligibility;
    private final PasswordEncoder passwords;
    private final EntityManager entities;
    private final AccountLifecycleGuard lifecycle;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long requestId) {
        Long userId = requests.findUserId(requestId).orElse(null);
        if (userId == null) return;

        // Same order as user-initiated writes: actor -> owned rooms -> request.
        lifecycle.lock(userId);
        User user = users.findById(userId).orElseThrow();
        entities.refresh(user);
        var ownedRooms = rooms.lockOwnedRooms(userId);
        ownedRooms.forEach(entities::refresh);
        AccountDeletionRequest request = requests.lockById(requestId).orElseThrow();
        if (!pending(request)) return;

        if (user.getStatus() == UserStatus.DELETED) {
            complete(request); // Safe retry after an already completed anonymization.
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE || user.isSuspended()) {
            fail(request, "ACCOUNT_INACTIVE");
            return;
        }
        try {
            eligibility.requireEligible(userId);
        } catch (ConflictException ex) {
            fail(request, ex.getCode());
            return;
        }

        request.setStatus(AccountDeletionRequest.DeletionStatus.PROCESSING);
        for (var room : ownedRooms) {
            listings.findByRoomId(room.getId()).ifPresent(listing -> {
                entities.refresh(listing);
                listing.setStatus(ListingStatus.ARCHIVED);
            });
        }
        sessions.revokeAllByUserId(userId);
        devices.deactivateAllByUserId(userId);
        verificationTokens.deleteByUserId(userId);
        wishlist.deleteByUserId(userId);
        anonymize(user);
        complete(request);
    }

    /** Called after an unexpected processing failure has rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long requestId) {
        requests.lockById(requestId).filter(AccountDeletionProcessor::pending)
                .ifPresent(request -> fail(request, "PROCESSING_ERROR"));
    }

    private void anonymize(User user) {
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        user.setFullName("Deleted user");
        user.setPhone(null);
        user.setAvatarUrl(null);
        user.setRefreshToken(null);
        user.setPassword(passwords.encode(UUID.randomUUID().toString()));
        user.setEmailVerified(false);
        user.setStatus(UserStatus.DELETED);
        user.setSuspended(false);
        user.setSuspendReason(null);
        user.getRoles().clear();
    }

    private static boolean pending(AccountDeletionRequest request) {
        return request.getStatus() == AccountDeletionRequest.DeletionStatus.PENDING
                || request.getStatus() == AccountDeletionRequest.DeletionStatus.PROCESSING;
    }

    private static void complete(AccountDeletionRequest request) {
        request.setStatus(AccountDeletionRequest.DeletionStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        request.setFailureCode(null);
        request.setReason("");
    }

    private static void fail(AccountDeletionRequest request, String code) {
        request.setStatus(AccountDeletionRequest.DeletionStatus.FAILED);
        request.setFailureCode(code);
        request.setUpdatedAt(Instant.now());
    }
}

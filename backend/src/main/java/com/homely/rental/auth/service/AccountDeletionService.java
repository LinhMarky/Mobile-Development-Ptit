package com.homely.rental.auth.service;

import com.homely.rental.auth.dto.request.DeletionRequest;
import com.homely.rental.auth.entity.AccountDeletionRequest;
import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.repository.AccountDeletionRequestRepository;
import com.homely.rental.auth.security.UserResolver;
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
    private final UserResolver userResolver;
    private final PasswordEncoder passwordEncoder;
    private final com.homely.rental.booking.repository.BookingRepository bookings;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public AccountDeletionRequest requestDeletion(DeletionRequest dto) throws IdInvalidException {
        User user = userResolver.requireCurrent();

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

        var activeBookings = List.of(com.homely.rental.booking.entity.BookingStatus.PENDING,
                com.homely.rental.booking.entity.BookingStatus.APPROVED, com.homely.rental.booking.entity.BookingStatus.CONFIRMED);
        boolean blocked = bookings.existsByTenantIdAndStatusInOrHostIdAndStatusIn(user.getId(), activeBookings, user.getId(), activeBookings);
        String[] checks = {
            "select count(v) from Viewing v where (v.tenant.id=:id or v.host.id=:id) and v.status in ('REQUESTED','CONFIRMED')",
            "select count(p) from Payment p where p.payer.id=:id and p.status='PENDING'",
            "select count(c) from BookingCase c where (c.booking.tenant.id=:id or c.booking.host.id=:id) and c.status in ('OPEN','IN_REVIEW')",
            "select count(r) from Refund r where (r.payment.booking.tenant.id=:id or r.payment.booking.host.id=:id) and r.status='PENDING'",
            "select count(r) from Room r where r.host.id=:id and r.availability in ('HELD','RENTED')"
        };
        for (String check : checks) blocked |= entityManager.createQuery(check,Long.class).setParameter("id",user.getId()).getSingleResult()>0;
        if (blocked) throw new ConflictException("DELETION_BLOCKED", "Resolve active bookings, payments, viewings and cases before deleting your account");

        AccountDeletionRequest request = new AccountDeletionRequest();
        request.setUser(user);
        request.setReason(dto.getReason() != null ? dto.getReason() : "");
        request.setStatus(AccountDeletionRequest.DeletionStatus.PENDING);

        return deletionRequestRepository.save(request);
    }
}

package com.homely.rental.auth.service;

import com.homely.rental.booking.entity.BookingStatus;
import com.homely.rental.booking.repository.BookingRepository;
import com.homely.rental.common.exception.ConflictException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Shared rules for accepting a request and rechecking it before anonymization. */
@Component
@RequiredArgsConstructor
public class AccountDeletionEligibility {
    private final BookingRepository bookings;
    private final EntityManager entities;

    public void requireEligible(Long userId) {
        if (hasActiveBookings(userId) || hasActiveViewings(userId) || hasPendingPayments(userId)
                || hasOpenCases(userId) || hasUnsettledRefunds(userId) || hasOccupiedRooms(userId)
                || hasOpenReports(userId)) {
            throw new ConflictException("DELETION_BLOCKED",
                    "Resolve active bookings, payments, viewings, refunds, reports and cases before deleting your account");
        }
    }

    private boolean hasActiveBookings(Long id) {
        var statuses = List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CONFIRMED);
        return bookings.existsByTenantIdAndStatusInOrHostIdAndStatusIn(id, statuses, id, statuses);
    }

    private boolean hasActiveViewings(Long id) {
        return exists("select count(v) from Viewing v where (v.tenant.id=:id or v.host.id=:id) and v.status in ('REQUESTED','CONFIRMED')", id);
    }

    private boolean hasPendingPayments(Long id) {
        return exists("select count(p) from Payment p where p.payer.id=:id and p.status='PENDING'", id);
    }

    private boolean hasOpenCases(Long id) {
        return exists("select count(c) from BookingCase c where (c.booking.tenant.id=:id or c.booking.host.id=:id) and c.status in ('OPEN','IN_REVIEW')", id);
    }

    private boolean hasUnsettledRefunds(Long id) {
        return exists("select count(r) from Refund r where (r.payment.booking.tenant.id=:id or r.payment.booking.host.id=:id) and r.status <> 'SUCCEEDED'", id);
    }

    private boolean hasOccupiedRooms(Long id) {
        return exists("select count(r) from Room r where r.host.id=:id and r.availability in ('HELD','RENTED')", id);
    }

    private boolean hasOpenReports(Long id) {
        return exists("select count(r) from Report r where r.reporter.id=:id and r.status in ('PENDING','REVIEWING')", id);
    }

    private boolean exists(String query, Long userId) {
        return entities.createQuery(query, Long.class).setParameter("id", userId).getSingleResult() > 0;
    }
}

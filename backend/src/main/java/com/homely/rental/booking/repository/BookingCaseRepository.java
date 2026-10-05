package com.homely.rental.booking.repository;

import com.homely.rental.booking.entity.BookingCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingCaseRepository extends JpaRepository<BookingCase, Long> {
    org.springframework.data.domain.Page<BookingCase> findByStatusInOrderByCreatedAtAsc(
            List<com.homely.rental.booking.entity.CaseStatus> statuses, org.springframework.data.domain.Pageable pageable);
    boolean existsByBookingIdAndStatusIn(Long bookingId, List<com.homely.rental.booking.entity.CaseStatus> statuses);
    List<BookingCase> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}

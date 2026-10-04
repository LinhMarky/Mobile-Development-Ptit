package com.homely.rental.booking.job;

import com.homely.rental.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for booking expiration (BR-13).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationJob {

    private final BookingService bookingService;

    /**
     * Expire PENDING bookings every minute.
     */
    @Scheduled(fixedRate = 60_000)
    public void expirePendingBookings() {
        bookingService.expirePendingBookings();
    }

    /**
     * Expire APPROVED bookings (hold timeout) every minute.
     */
    @Scheduled(fixedRate = 60_000)
    public void expireApprovedBookings() {
        bookingService.expireApprovedBookings();
    }

    /**
     * Check for overdue handovers every minute.
     */
    @Scheduled(fixedRate = 60_000)
    public void handleOverdueHandovers() {
        bookingService.handleOverdueHandovers();
    }
}

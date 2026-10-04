package com.homely.rental.booking.service;

import com.homely.rental.booking.entity.Booking;
import com.homely.rental.booking.repository.BookingRepository;
import com.homely.rental.catalog.repository.RoomRepository;
import com.homely.rental.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** All booking/payment mutations acquire locks in room -> booking -> payment order. */
@Component
@RequiredArgsConstructor
public class BookingLocks {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public Booking lock(Long id) {
        Long roomId = bookings.findRoomId(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        var room = rooms.lockById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        entityManager.refresh(room);
        var booking = bookings.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        entityManager.refresh(booking);
        return booking;
    }
}

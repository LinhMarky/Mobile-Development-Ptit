package com.homely.rental.booking.repository;

import com.homely.rental.booking.entity.Booking;
import com.homely.rental.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("select b.room.id from Booking b where b.id = :id")
    Optional<Long> findRoomId(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> lockById(Long id);

    boolean existsByTenantIdAndRoomIdAndStatusIn(Long tenantId, Long roomId, List<BookingStatus> statuses);
    boolean existsByRoomIdAndStatusIn(Long roomId, List<BookingStatus> statuses);
    boolean existsByTenantIdAndStatusInOrHostIdAndStatusIn(Long tenantId, List<BookingStatus> tenantStatuses,
                                                         Long hostId, List<BookingStatus> hostStatuses);

    Page<Booking> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<Booking> findByHostIdOrderByCreatedAtDesc(Long hostId, Pageable pageable);

    Optional<Booking> findByIdAndHostId(Long id, Long hostId);

    Optional<Booking> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.requestExpiresAt <= :now ORDER BY b.room.id, b.id")
    List<Booking> findExpiredPendingBookings(Instant now);

    @Query("SELECT b FROM Booking b WHERE b.status = 'APPROVED' AND b.holdExpiresAt <= :now ORDER BY b.room.id, b.id")
    List<Booking> findExpiredApprovedBookings(Instant now);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.handoverDueAt <= :now ORDER BY b.room.id, b.id")
    List<Booking> findOverdueHandovers(Instant now);
}

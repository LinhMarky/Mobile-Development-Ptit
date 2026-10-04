package com.homely.rental.payment.repository;

import com.homely.rental.payment.entity.Payment;
import com.homely.rental.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDescIdDesc(Long bookingId);
    default Optional<Payment> findByBookingId(Long bookingId) {
        return findFirstByBookingIdOrderByCreatedAtDescIdDesc(bookingId);
    }
    @org.springframework.data.jpa.repository.Query("select p.booking.id from Payment p where p.providerPaymentId = :providerId")
    Optional<Long> findBookingId(String providerId);
    @org.springframework.data.jpa.repository.Query("select p.booking.id from Payment p where p.id = :paymentId")
    Optional<Long> findBookingIdByPaymentId(Long paymentId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.id = :paymentId")
    Optional<Payment> lockById(Long paymentId);
    @org.springframework.data.jpa.repository.Query("select p.id from Payment p where p.status = :status and p.expiresAt <= :cutoff order by p.expiresAt, p.id")
    List<Long> findDueIds(PaymentStatus status, Instant cutoff, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.providerPaymentId = :providerId")
    Optional<Payment> lockByProviderId(String providerId);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}

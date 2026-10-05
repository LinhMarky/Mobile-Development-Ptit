package com.homely.rental.payment.job;

import com.homely.rental.payment.entity.PaymentStatus;
import com.homely.rental.payment.repository.PaymentRepository;
import com.homely.rental.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Bounded scan; independent transactions keep one failed attempt from blocking the batch. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationJob {
    private final PaymentRepository payments;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${homely.payment.expiration-poll-ms:60000}")
    public void expirePendingPayments() {
        Instant cutoff = Instant.now();
        for (Long id : payments.findDueIds(PaymentStatus.PENDING, cutoff, PageRequest.of(0, 100))) {
            try {
                paymentService.expirePayment(id, cutoff);
            } catch (RuntimeException exception) {
                log.warn("Could not expire payment {}; it will be retried on the next scan", id, exception);
            }
        }
    }
}

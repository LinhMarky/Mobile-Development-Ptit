package com.homely.rental.payment;

import com.homely.rental.payment.entity.PaymentStatus;
import com.homely.rental.payment.job.PaymentExpirationJob;
import com.homely.rental.payment.repository.PaymentRepository;
import com.homely.rental.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PaymentExpirationJobTest {
    @Test void batchIsBoundedAndOneFailureDoesNotBlockRemainingCandidates() {
        PaymentRepository payments = mock(PaymentRepository.class); PaymentService service = mock(PaymentService.class);
        when(payments.findDueIds(eq(PaymentStatus.PENDING), any(), any())).thenReturn(List.of(1L, 2L));
        when(service.expirePayment(eq(1L), any())).thenThrow(new IllegalStateException("Temporary failure"));
        new PaymentExpirationJob(payments, service).expirePendingPayments();
        var cutoff = ArgumentCaptor.forClass(Instant.class); var page = ArgumentCaptor.forClass(Pageable.class);
        verify(payments).findDueIds(eq(PaymentStatus.PENDING), cutoff.capture(), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(100);
        verify(service).expirePayment(1L, cutoff.getValue()); verify(service).expirePayment(2L, cutoff.getValue());
    }
}

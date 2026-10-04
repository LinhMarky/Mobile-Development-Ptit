package com.homely.rental.payment.repository;
import com.homely.rental.payment.entity.WebhookReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface WebhookReceiptRepository extends JpaRepository<WebhookReceipt,Long> {
    Optional<WebhookReceipt> findByProviderAndEventId(String provider, String eventId);
}


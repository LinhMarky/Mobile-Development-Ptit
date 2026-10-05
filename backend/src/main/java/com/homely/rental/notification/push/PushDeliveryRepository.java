package com.homely.rental.notification.push;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface PushDeliveryRepository extends JpaRepository<PushDelivery,Long> {
    @Query("select d.id from PushDelivery d where d.status in ('PENDING','PROCESSING') and d.nextAttemptAt <= :now order by d.id")
    List<Long> due(Instant now, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from PushDelivery d where d.id=:id")
    Optional<PushDelivery> lock(Long id);
}

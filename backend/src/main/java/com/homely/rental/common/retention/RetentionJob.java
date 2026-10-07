package com.homely.rental.common.retention;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "homely.retention.enabled", havingValue = "true", matchIfMissing = true)
public class RetentionJob {
    private final RetentionService retention;
    private final ObjectProvider<MeterRegistry> meters;

    @Scheduled(fixedDelayString = "${homely.retention.poll-ms:60000}")
    public void tick() {
        try {
            var result = retention.purgeBatch();
            if (result.total() == 0) return;
            MeterRegistry registry = meters.getIfAvailable();
            if (registry != null) registry.counter("homely.retention.deleted").increment(result.total());
            log.info("Retention deleted: sessions={}, tokens={}, idempotency={}, pushes={}",
                    result.sessions(), result.tokens(), result.idempotency(), result.pushes());
        } catch (RuntimeException ex) {
            MeterRegistry registry = meters.getIfAvailable();
            if (registry != null) registry.counter("homely.retention.failures").increment();
            log.error("Retention batch failed ({})", ex.getClass().getSimpleName());
        }
    }
}

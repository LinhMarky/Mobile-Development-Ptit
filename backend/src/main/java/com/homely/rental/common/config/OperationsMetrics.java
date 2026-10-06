package com.homely.rental.common.config;

import com.homely.rental.notification.push.PushDeliveryRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Gauges expose counts only; notification contents and device tokens stay private. */
@Component
@RequiredArgsConstructor
public class OperationsMetrics implements MeterBinder {
    private final PushDeliveryRepository pushes;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("homely.push.backlog", pushes,
                repository -> repository.countByStatusIn(List.of("PENDING", "PROCESSING")))
                .description("Push deliveries waiting for delivery or retry").register(registry);
        Gauge.builder("homely.push.failed", pushes,
                repository -> repository.countByStatusIn(List.of("FAILED")))
                .description("Failed push deliveries retained for investigation").register(registry);
    }
}

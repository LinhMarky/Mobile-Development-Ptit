package com.homely.rental.common.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Avoid accidentally exposing demo payment or demo accounts in production. */
@Component
@Profile("prod")
public class ProductionConfigurationGuard implements InitializingBean {
    @Value("${homely.demo.enabled:false}") private boolean demoEnabled;
    @Value("${homely.demo.seed:false}") private boolean demoSeed;
    @Value("${homely.payment.mock-enabled:false}") private boolean mockPaymentEnabled;

    @Override
    public void afterPropertiesSet() {
        if (demoEnabled || demoSeed || mockPaymentEnabled) {
            throw new IllegalStateException("Production profile forbids DEMO_ENABLED, DEMO_SEED and MOCK_PAYMENT_ENABLED");
        }
    }
}

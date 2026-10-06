package com.homely.rental.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ProductionConfigurationGuardTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withInitializer(application -> application.getEnvironment().setActiveProfiles("prod"))
            .withUserConfiguration(ProductionConfigurationGuard.class);

    @Test void rejectsEachDemoSwitchInProduction() {
        for (String property : java.util.List.of("homely.demo.enabled", "homely.demo.seed", "homely.payment.mock-enabled")) {
            context.withPropertyValues(property + "=true").run(application ->
                    assertThat(application.getStartupFailure()).hasRootCauseMessage(
                            "Production profile forbids DEMO_ENABLED, DEMO_SEED and MOCK_PAYMENT_ENABLED"));
        }
    }

    @Test void acceptsProductionWithoutDemoOrMockPayments() {
        context.withPropertyValues("homely.demo.enabled=false", "homely.demo.seed=false", "homely.payment.mock-enabled=false")
                .run(application -> assertThat(application).hasNotFailed());
    }
}

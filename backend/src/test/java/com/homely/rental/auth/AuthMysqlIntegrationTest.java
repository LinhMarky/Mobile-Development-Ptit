package com.homely.rental.auth;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Opt-in: rerun the complete auth flow against a dedicated MySQL test schema. */
@EnabledIfEnvironmentVariable(named="HOMELY_TEST_MYSQL_URL", matches=".+")
@SpringBootTest(properties={
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true", "homely.demo.enabled=false", "homely.demo.seed=false", "homely.fcm.enabled=false",
        "homely.payment.mock-enabled=false", "logging.level.org.hibernate.SQL=OFF", "management.health.mail.enabled=false",
        "logging.level.org.springframework.web=INFO", "logging.level.org.springframework.security=INFO", "debug=false"})
class AuthMysqlIntegrationTest extends AuthIntegrationTest {
    @DynamicPropertySource static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("HOMELY_TEST_MYSQL_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("HOMELY_TEST_MYSQL_USER", "root"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("HOMELY_TEST_MYSQL_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}

package com.homely.rental.common.retention;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named="HOMELY_TEST_MYSQL_URL", matches=".+")
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties={"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"}, showSql=false)
class RetentionMysqlTest extends RetentionIntegrationTest {
    @DynamicPropertySource static void mysql(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("HOMELY_TEST_MYSQL_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("HOMELY_TEST_MYSQL_USER", "root"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("HOMELY_TEST_MYSQL_PASSWORD", ""));
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}

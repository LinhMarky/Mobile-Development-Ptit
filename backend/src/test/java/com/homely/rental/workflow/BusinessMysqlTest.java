package com.homely.rental.workflow;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Runs the same workflow suite on an explicitly selected isolated MySQL test schema. */
@EnabledIfEnvironmentVariable(named="HOMELY_TEST_MYSQL_URL", matches=".+")
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties={"spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED", "homely.fcm.enabled=true"}, showSql=false)
class BusinessMysqlTest extends BusinessWorkflowTest {
    @DynamicPropertySource static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",()->System.getenv("HOMELY_TEST_MYSQL_URL"));
        registry.add("spring.datasource.username",()->System.getenv().getOrDefault("HOMELY_TEST_MYSQL_USER","root"));
        registry.add("spring.datasource.password",()->System.getenv().getOrDefault("HOMELY_TEST_MYSQL_PASSWORD",""));
        registry.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");
    }
}

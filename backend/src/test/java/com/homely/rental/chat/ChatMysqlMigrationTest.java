package com.homely.rental.chat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Opt-in only: provide a dedicated, empty test schema; never runs against the application database. */
@EnabledIfEnvironmentVariable(named = "HOMELY_TEST_MYSQL_URL", matches = ".+")
class ChatMysqlMigrationTest {
    @Test void flywayMigratesRealMysqlAndEnforcesChatUniqueness() throws Exception {
        String url = System.getenv("HOMELY_TEST_MYSQL_URL");
        String username = System.getenv().getOrDefault("HOMELY_TEST_MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("HOMELY_TEST_MYSQL_PASSWORD", "");
        Flyway flyway = Flyway.configure().dataSource(url, username, password)
                .locations("classpath:db/migration").cleanDisabled(true).load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).anyMatch(info -> info.getVersion() != null
                && "11".equals(info.getVersion().toString()));

        try (var connection = DriverManager.getConnection(url, username, password);
             var sql = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                sql.executeUpdate("insert into users (id,email,password,full_name) values "
                        + "(91001,'phase7-host@example.test','unused-test-hash','Host'),"
                        + "(91002,'phase7-tenant@example.test','unused-test-hash','Tenant')");
                sql.executeUpdate("insert into rooms (id,host_id,unit_code,room_type,area_m2,max_occupants) "
                        + "values (91001,91001,'CHAT-TEST','SINGLE_ROOM',20,2)");
                sql.executeUpdate("insert into conversations (id,room_id,tenant_id,host_id) values (91001,91001,91002,91001)");
                assertThatThrownBy(() -> sql.executeUpdate("insert into conversations (room_id,tenant_id,host_id) "
                        + "values (91001,91002,91001)"))
                        .isInstanceOf(SQLException.class);
                sql.executeUpdate("insert into messages (conversation_id,sender_id,content,client_message_id,sequence) "
                        + "values (91001,91002,'hello','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',1)");
                assertThatThrownBy(() -> sql.executeUpdate("insert into messages "
                        + "(conversation_id,sender_id,content,client_message_id,sequence) "
                        + "values (91001,91002,'duplicate','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',2)"))
                        .isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> sql.executeUpdate("insert into messages "
                        + "(conversation_id,sender_id,content,client_message_id,sequence) "
                        + "values (91001,91002,'same sequence','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',1)"))
                        .isInstanceOf(SQLException.class);
                try (var count = sql.executeQuery("select count(*) from messages where conversation_id=91001")) {
                    assertThat(count.next()).isTrue();
                    assertThat(count.getInt(1)).isEqualTo(1);
                }
            } finally {
                connection.rollback();
            }
        }
    }
}

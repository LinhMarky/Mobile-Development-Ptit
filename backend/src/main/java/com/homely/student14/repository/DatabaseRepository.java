package com.homely.student14.repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DatabaseRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    DatabaseRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Object readJsonResource(String resourcePath) {
        try (InputStream resourceStream = getClass().getResourceAsStream(resourcePath)) {
            return objectMapper.readValue(resourceStream, Object.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Missing JSON resource: " + resourcePath, exception);
        }
    }

    public List<Map<String, Object>> all(String query, Object... arguments) {
        return jdbcTemplate.query(query, (resultSet, rowNumber) -> readRow(resultSet), arguments);
    }

    private Map<String, Object> readRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            String columnName = metadata.getColumnLabel(columnIndex);
            Object databaseValue = resultSet.getObject(columnIndex);
            row.put(columnName, normalizeValue(databaseValue));
        }
        return row;
    }

    private Object normalizeValue(Object databaseValue) {
        if (databaseValue instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (databaseValue instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC).toString();
        }
        if (databaseValue instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (databaseValue instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return databaseValue;
    }

    public Map<String, Object> one(String query, Object... arguments) {
        List<Map<String, Object>> rows = all(query, arguments);
        if (rows.isEmpty()) {
            throw fail(404, "Không tìm thấy dữ liệu.");
        }
        return rows.get(0);
    }

    public long insert(String query, Object... arguments) {
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    for (int argumentIndex = 0; argumentIndex < arguments.length; argumentIndex++) {
                        statement.setObject(argumentIndex + 1, arguments[argumentIndex]);
                    }
                    return statement;
                },
                generatedKeyHolder);
        Number generatedId = Objects.requireNonNull(generatedKeyHolder.getKey());
        return generatedId.longValue();
    }

    public int update(String query, Object... arguments) {
        return jdbcTemplate.update(query, arguments);
    }

    public String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw fail(400, "Dữ liệu không hợp lệ.");
        }
    }

    public Object decode(Object storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(storedValue.toString(), Object.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stored JSON", exception);
        }
    }

    public static long id(Object value) {
        try {
            long parsedId = Long.parseLong(Objects.toString(value, ""));
            if (parsedId > 0) {
                return parsedId;
            }
        } catch (NumberFormatException ignored) {
            // Báo cùng một lỗi cho ID thiếu, sai định dạng hoặc không dương.
        }
        throw fail(400, "Mã dữ liệu không hợp lệ.");
    }

    public static String text(
            Map<String, Object> requestBody, String fieldName, int maxLength, boolean required) {
        String value = Objects.toString(requestBody.get(fieldName), "").trim();
        boolean missingRequiredValue = required && value.isEmpty();
        boolean exceedsLength = value.codePointCount(0, value.length()) > maxLength;
        if (missingRequiredValue || exceedsLength) {
            throw fail(
                    400, "Trường " + fieldName + " không hợp lệ (tối đa " + maxLength + " ký tự).");
        }
        return value;
    }

    public static boolean flag(Object value) {
        return Boolean.TRUE.equals(value) || "1".equals(Objects.toString(value));
    }

    public static void require(boolean condition, int statusCode, String message) {
        if (!condition) {
            throw fail(statusCode, message);
        }
    }

    public static ResponseStatusException fail(int statusCode, String message) {
        return new ResponseStatusException(HttpStatus.valueOf(statusCode), message);
    }

    public static Map<String, Object> page(List<?> items) {
        return Map.of(
                "data",
                items,
                "total_elements",
                items.size(),
                "total_pages",
                items.isEmpty() ? 0 : 1,
                "current_page",
                0,
                "page_size",
                items.size());
    }

    public void notify(
            long userId,
            String notificationType,
            String title,
            String body,
            String referenceType,
            long referenceId) {
        insert(
                """
                INSERT INTO notifications(
                    user_id, notification_type, title, body, ref_type, ref_id
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                userId,
                notificationType,
                title,
                body,
                referenceType,
                referenceId);
    }
}

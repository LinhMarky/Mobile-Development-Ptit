package com.homely.student14.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AdminRepository {
    private final DatabaseRepository databaseRepository;

    public List<Map<String, Object>> findAllListings() {
        return databaseRepository.all(CatalogRepository.LISTING_SELECT + "ORDER BY l.id DESC");
    }

    public List<Map<String, Object>> findAllUsers() {
        return databaseRepository.all("SELECT * FROM users ORDER BY id DESC");
    }

    public Map<String, Object> getSummary() {
        return databaseRepository.one(
                """
SELECT DATABASE() AS database_name,
       (SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE') AS table_count,
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM listings WHERE status = 'PENDING_REVIEW') AS pending_listings,
       (SELECT COUNT(*) FROM bookings WHERE status = 'COMPLETED') AS completed_bookings
""");
    }

    public void updateAccountStatus(long userId, String status, String reason) {
        databaseRepository.one("SELECT id FROM users WHERE id = ?", userId);
        databaseRepository.update(
                """
                UPDATE users SET status = ?, suspend_reason = ? WHERE id = ?
                """,
                status,
                reason,
                userId);
    }

    public void revokeSessions(long userId) {
        databaseRepository.update(
                """
                UPDATE auth_tokens SET consumed_at = UTC_TIMESTAMP(3)
                WHERE user_id = ? AND consumed_at IS NULL
                """,
                userId);
    }
}

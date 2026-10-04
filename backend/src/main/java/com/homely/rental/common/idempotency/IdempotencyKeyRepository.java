package com.homely.rental.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {
    Optional<IdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);

    @Transactional
    @Modifying
    @Query("DELETE FROM IdempotencyKeyEntity e WHERE e.idempotencyKey = ?1 AND e.status = ?2 AND e.expiresAt <= ?3")
    int deleteExpiredKey(String key, IdempotencyKeyEntity.IdempotencyStatus status, Instant now);
}

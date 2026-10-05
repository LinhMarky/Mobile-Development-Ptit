package com.homely.rental.auth.repository;

import com.homely.rental.auth.entity.OneTimeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<OneTimeToken> findByTokenHashAndUsedFalse(String tokenHash);

    @Modifying
    @Query("DELETE FROM OneTimeToken t WHERE t.expiresAt < ?1")
    int deleteExpiredTokens(Instant now);
}

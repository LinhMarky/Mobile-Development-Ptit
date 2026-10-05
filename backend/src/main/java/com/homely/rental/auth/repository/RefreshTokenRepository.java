package com.homely.rental.auth.repository;

import com.homely.rental.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> lockByHash(String hash);
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = ?1 AND rt.revoked = false")
    int revokeAllByUserId(Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = ?1 AND rt.installationId = ?2 AND rt.revoked = false")
    int revokeByUserIdAndInstallationId(Long userId, String installationId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < ?1")
    int deleteExpiredTokens(Instant now);
}

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

    void deleteByUserId(Long userId);

    @Query("select t.id from OneTimeToken t where t.expiresAt < :now order by t.expiresAt, t.id")
    java.util.List<Long> findExpiredIds(Instant now, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("delete from OneTimeToken t where t.id in :ids and t.expiresAt < :now")
    int deleteExpiredIds(java.util.List<Long> ids, Instant now);
}

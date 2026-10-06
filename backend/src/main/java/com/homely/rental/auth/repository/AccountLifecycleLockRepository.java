package com.homely.rental.auth.repository;

import com.homely.rental.auth.entity.AccountLifecycleLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountLifecycleLockRepository extends JpaRepository<AccountLifecycleLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from AccountLifecycleLock l where l.id = :userId")
    Optional<AccountLifecycleLock> lock(Long userId);
}

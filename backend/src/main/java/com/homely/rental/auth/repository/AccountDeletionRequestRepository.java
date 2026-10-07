package com.homely.rental.auth.repository;

import com.homely.rental.auth.entity.AccountDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {
    Optional<AccountDeletionRequest> findByUserId(Long userId);
    boolean existsByUserIdAndStatusIn(Long userId, java.util.Collection<AccountDeletionRequest.DeletionStatus> statuses);

    @org.springframework.data.jpa.repository.Query("select r.user.id from AccountDeletionRequest r where r.id = :id")
    Optional<Long> findUserId(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from AccountDeletionRequest r where r.id = :id")
    Optional<AccountDeletionRequest> lockById(Long id);

    @org.springframework.data.jpa.repository.Query("select r.id from AccountDeletionRequest r where r.status in ('PENDING','PROCESSING') order by r.id")
    java.util.List<Long> findPendingIds(org.springframework.data.domain.Pageable pageable);
}

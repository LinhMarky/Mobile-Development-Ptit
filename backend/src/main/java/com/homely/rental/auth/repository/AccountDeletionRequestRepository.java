package com.homely.rental.auth.repository;

import com.homely.rental.auth.entity.AccountDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {
    Optional<AccountDeletionRequest> findByUserId(Long userId);
    boolean existsByUserIdAndStatusIn(Long userId, java.util.Collection<AccountDeletionRequest.DeletionStatus> statuses);
}

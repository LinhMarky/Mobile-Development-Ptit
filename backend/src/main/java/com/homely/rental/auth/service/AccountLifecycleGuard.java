package com.homely.rental.auth.service;

import com.homely.rental.auth.repository.AccountLifecycleLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Every actor mutation and deletion acquires this lock before resource locks. */
@Component
@RequiredArgsConstructor
public class AccountLifecycleGuard {
    private final AccountLifecycleLockRepository locks;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(Long userId) {
        locks.lock(userId).orElseThrow(() -> new IllegalStateException("Account lifecycle lock is missing"));
    }
}

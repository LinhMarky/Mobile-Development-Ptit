package com.homely.rental.auth.job;

import com.homely.rental.auth.repository.AccountDeletionRequestRepository;
import com.homely.rental.auth.service.AccountDeletionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "homely.account-deletion.enabled", havingValue = "true", matchIfMissing = true)
public class AccountDeletionJob {
    private final AccountDeletionRequestRepository requests;
    private final AccountDeletionProcessor processor;

    @Scheduled(fixedDelayString = "${homely.account-deletion.poll-ms:30000}")
    public void tick() {
        for (Long id : requests.findPendingIds(PageRequest.of(0, 50))) {
            try {
                processor.process(id);
            } catch (RuntimeException ex) {
                log.error("Account deletion request {} failed ({})", id, ex.getClass().getSimpleName());
                try {
                    processor.markFailed(id);
                } catch (RuntimeException stateFailure) {
                    // Leave PENDING for a later retry if the database is temporarily unavailable.
                    log.error("Could not record deletion failure for request {} ({})", id, stateFailure.getClass().getSimpleName());
                }
            }
        }
    }
}

package com.homely.rental.common.retention;

import com.homely.rental.auth.repository.OneTimeTokenRepository;
import com.homely.rental.auth.repository.RefreshTokenRepository;
import com.homely.rental.common.idempotency.IdempotencyKeyRepository;
import com.homely.rental.notification.push.PushDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Only disposable data is purged. Business history and in-progress work are retained. */
@Service
@RequiredArgsConstructor
public class RetentionService {
    private final RefreshTokenRepository sessions;
    private final OneTimeTokenRepository tokens;
    private final IdempotencyKeyRepository idempotency;
    private final PushDeliveryRepository pushes;
    private final RetentionProperties properties;

    public record Result(int sessions, int tokens, int idempotency, int pushes) {
        public int total() { return sessions + tokens + idempotency + pushes; }
    }

    @Transactional
    public Result purgeBatch() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(properties.getPushDays(), ChronoUnit.DAYS);
        PageRequest batch = PageRequest.of(0, properties.getBatchSize());

        var sessionIds = sessions.findExpiredIds(now, batch);
        var tokenIds = tokens.findExpiredIds(now, batch);
        var idempotencyIds = idempotency.findExpiredCompletedIds(now, batch);
        var pushIds = pushes.findRetainedTerminalIds(cutoff, batch);

        return new Result(
                sessionIds.isEmpty() ? 0 : sessions.deleteExpiredIds(sessionIds, now),
                tokenIds.isEmpty() ? 0 : tokens.deleteExpiredIds(tokenIds, now),
                idempotencyIds.isEmpty() ? 0 : idempotency.deleteExpiredCompletedIds(idempotencyIds, now),
                pushIds.isEmpty() ? 0 : pushes.deleteRetainedTerminalIds(pushIds, cutoff));
    }
}

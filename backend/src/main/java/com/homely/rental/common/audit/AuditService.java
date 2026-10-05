package com.homely.rental.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homely.rental.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository logs;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User actor, String action, String targetType, Long targetId, Map<String, ?> detail) {
        try {
            logs.save(AuditLog.builder().actor(actor).actorType("ADMIN").action(action)
                    .targetType(targetType).targetId(targetId).detail(mapper.writeValueAsString(detail))
                    .traceId(UUID.randomUUID().toString()).createdAt(Instant.now().truncatedTo(ChronoUnit.MILLIS)).build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize audit record", ex);
        }
    }
}

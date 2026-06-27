package com.company.officerservice.infrastructure.persistence.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRelayRepository {
    OutboxEventEntity save(OutboxEventEntity entity);
    Optional<OutboxEventEntity> findById(UUID id);
    Optional<OutboxEventEntity> findPendingByIdWithLock(UUID id);
    List<OutboxEventEntity> findPending(int batchSize);
}

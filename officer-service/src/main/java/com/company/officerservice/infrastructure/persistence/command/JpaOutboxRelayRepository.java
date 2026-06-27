package com.company.officerservice.infrastructure.persistence.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaOutboxRelayRepository implements OutboxRelayRepository {

    private final OutboxEventEntityRepository delegate;

    public JpaOutboxRelayRepository(OutboxEventEntityRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public OutboxEventEntity save(OutboxEventEntity entity) {
        return delegate.save(entity);
    }

    @Override
    public Optional<OutboxEventEntity> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<OutboxEventEntity> findPendingByIdWithLock(UUID id) {
        return delegate.findPendingByIdWithLock(id);
    }

    @Override
    public List<OutboxEventEntity> findPending(int batchSize) {
        return delegate.findPending(batchSize);
    }
}

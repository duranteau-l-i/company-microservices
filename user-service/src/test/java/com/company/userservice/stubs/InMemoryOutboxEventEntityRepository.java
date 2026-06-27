package com.company.userservice.stubs;

import com.company.userservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.userservice.infrastructure.persistence.command.OutboxRelayRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryOutboxEventEntityRepository implements OutboxRelayRepository {

    private final Map<UUID, OutboxEventEntity> store = new LinkedHashMap<>();

    public OutboxEventEntity save(OutboxEventEntity entity) {
        store.put(entity.getId(), entity);
        return entity;
    }

    public Optional<OutboxEventEntity> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<OutboxEventEntity> findPending(int batchSize) {
        return store.values().stream()
                .filter(e -> "PENDING".equals(e.getStatus()))
                .sorted(Comparator.comparing(OutboxEventEntity::getCreatedAt))
                .limit(batchSize)
                .toList();
    }

    public Optional<OutboxEventEntity> findPendingByIdWithLock(UUID id) {
        return findById(id).filter(e -> "PENDING".equals(e.getStatus()));
    }

    public List<OutboxEventEntity> all() {
        return List.copyOf(store.values());
    }

    public void clear() {
        store.clear();
    }
}

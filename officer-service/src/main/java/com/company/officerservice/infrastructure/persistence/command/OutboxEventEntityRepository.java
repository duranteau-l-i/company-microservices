package com.company.officerservice.infrastructure.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventEntityRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :batchSize", nativeQuery = true)
    List<OutboxEventEntity> findPending(@Param("batchSize") int batchSize);

    @Query(value = "SELECT * FROM outbox_events WHERE id = :id AND status = 'PENDING' FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<OutboxEventEntity> findPendingByIdWithLock(@Param("id") UUID id);
}

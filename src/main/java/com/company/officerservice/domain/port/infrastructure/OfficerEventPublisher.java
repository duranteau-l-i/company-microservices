package com.company.officerservice.domain.port.infrastructure;

import com.company.officerservice.domain.event.DomainEvent;

public interface OfficerEventPublisher {
    void publish(DomainEvent event);
}

package com.company.userservice.domain.port.infrastructure;

import com.company.userservice.domain.event.DomainEvent;

public interface UserEventPublisher {
    void publish(DomainEvent event);
}

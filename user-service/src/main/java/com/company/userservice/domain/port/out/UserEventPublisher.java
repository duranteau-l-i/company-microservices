package com.company.userservice.domain.port.out;

import com.company.userservice.domain.event.DomainEvent;

public interface UserEventPublisher {
    void publish(DomainEvent event);
}

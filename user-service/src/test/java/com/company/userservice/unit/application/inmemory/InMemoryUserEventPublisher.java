package com.company.userservice.unit.application.inmemory;

import com.company.userservice.domain.event.DomainEvent;
import com.company.userservice.domain.port.infrastructure.UserEventPublisher;

import java.util.ArrayList;
import java.util.List;

public class InMemoryUserEventPublisher implements UserEventPublisher {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> publishedEvents() {
        return List.copyOf(events);
    }

    public DomainEvent lastEvent() {
        if (events.isEmpty()) {
            throw new IllegalStateException("No events have been published");
        }

        return events.get(events.size() - 1);
    }

    public void clear() {
        events.clear();
    }
}

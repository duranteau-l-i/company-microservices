package com.company.companyservice.unit.application.stubs;

import com.company.companyservice.domain.event.DomainEvent;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class InMemoryCompanyEventPublisher implements CompanyEventPublisher {

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
            throw new NoSuchElementException("No events published");
        }
        return events.get(events.size() - 1);
    }

    public void clear() {
        events.clear();
    }
}

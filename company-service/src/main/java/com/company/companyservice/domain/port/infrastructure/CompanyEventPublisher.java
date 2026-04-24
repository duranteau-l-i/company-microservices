package com.company.companyservice.domain.port.infrastructure;

import com.company.companyservice.domain.event.DomainEvent;

public interface CompanyEventPublisher {
    void publish(DomainEvent event);
}

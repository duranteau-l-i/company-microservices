package com.company.companyservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyFullView(
        CompanyId id,
        String name,
        String registrationNumber,
        Address address,
        UUID ownerId,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<OfficerSummary> officers
) implements CompanyView {
}

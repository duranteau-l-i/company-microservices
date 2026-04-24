package com.company.companyservice.domain.model;

import java.util.UUID;

public record CompanyRestrictedView(
        CompanyId id,
        String name,
        String registrationNumber,
        UUID ownerId,
        String ownerDisplayName,
        CompanyStatus status
) implements CompanyView {
}

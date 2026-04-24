package com.company.companyservice.infrastructure.feign;

import java.util.List;
import java.util.UUID;

public record OfficerClientDto(
        UUID id,
        String firstName,
        String lastName,
        List<CompanyLinkDto> companyLinks
) {
    public record CompanyLinkDto(UUID companyId, String title) {}
}

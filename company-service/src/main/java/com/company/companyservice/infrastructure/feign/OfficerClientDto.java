package com.company.companyservice.infrastructure.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OfficerClientDto(
        UUID id,
        String firstName,
        String lastName,
        List<OfficerCompanyLinkDto> companyLinks
) {}

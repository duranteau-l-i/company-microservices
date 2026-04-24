package com.company.officerservice.presentation.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OfficerFullResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationality,
        AddressResponse address,
        String email,
        String phone,
        List<CompanyLinkResponse> companyLinks,
        Instant createdAt,
        Instant updatedAt
) {}

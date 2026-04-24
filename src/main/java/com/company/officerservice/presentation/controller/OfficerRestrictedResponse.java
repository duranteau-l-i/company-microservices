package com.company.officerservice.presentation.controller;

import java.util.List;
import java.util.UUID;

public record OfficerRestrictedResponse(
        UUID id,
        String firstName,
        String lastName,
        List<CompanyLinkResponse> companyLinks
) {}

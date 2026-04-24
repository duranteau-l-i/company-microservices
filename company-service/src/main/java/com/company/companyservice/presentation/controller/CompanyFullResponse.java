package com.company.companyservice.presentation.controller;

import java.util.List;

public record CompanyFullResponse(
        String id,
        String name,
        String registrationNumber,
        AddressResponse address,
        String ownerId,
        String ownerDisplayName,
        String status,
        String createdAt,
        String updatedAt,
        List<OfficerSummaryResponse> officers,
        List<String> warnings
) {
}

package com.company.companyservice.presentation.rest;

public record CompanyRestrictedResponse(
        String id,
        String name,
        String registrationNumber,
        String ownerId,
        String ownerDisplayName,
        String status
) {
}

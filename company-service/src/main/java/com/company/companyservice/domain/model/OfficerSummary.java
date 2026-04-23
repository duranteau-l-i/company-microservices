package com.company.companyservice.domain.model;

import java.util.UUID;

public record OfficerSummary(UUID officerId, String firstName, String lastName, String title) {
}

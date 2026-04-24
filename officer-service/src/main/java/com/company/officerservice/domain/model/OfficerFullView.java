package com.company.officerservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OfficerFullView(
        OfficerId id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationality,
        Address address,
        String email,
        String phone,
        List<CompanyLink> companyLinks,
        Instant createdAt,
        Instant updatedAt
) implements OfficerView {
}

package com.company.officerservice.domain.model;

import java.util.List;

public record OfficerRestrictedView(
        OfficerId id,
        String firstName,
        String lastName,
        List<CompanyLink> companyLinks
) implements OfficerView {
}

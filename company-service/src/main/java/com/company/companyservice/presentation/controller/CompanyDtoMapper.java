package com.company.companyservice.presentation.controller;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.OfficerSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompanyDtoMapper {

    public CompanyFullResponse toFullResponse(CompanyFullView view) {
        return toFullResponse(view, List.of());
    }

    public CompanyFullResponse toFullResponse(CompanyFullView view, List<String> warnings) {
        AddressResponse addressResponse = new AddressResponse(
                view.address().street(),
                view.address().city(),
                view.address().postalCode(),
                view.address().country()
        );

        List<OfficerSummaryResponse> officers = view.officers() == null
                ? List.of()
                : view.officers().stream().map(this::toOfficerSummaryResponse).toList();

        return new CompanyFullResponse(
                view.id().value().toString(),
                view.name(),
                view.registrationNumber(),
                addressResponse,
                view.ownerId().toString(),
                view.ownerDisplayName(),
                view.status().name(),
                view.createdAt().toString(),
                view.updatedAt().toString(),
                officers,
                warnings
        );
    }

    public CompanyRestrictedResponse toRestrictedResponse(CompanyRestrictedView view) {
        return new CompanyRestrictedResponse(
                view.id().value().toString(),
                view.name(),
                view.registrationNumber(),
                view.ownerId().toString(),
                view.ownerDisplayName(),
                view.status().name()
        );
    }

    private OfficerSummaryResponse toOfficerSummaryResponse(OfficerSummary officer) {
        return new OfficerSummaryResponse(
                officer.officerId().toString(),
                officer.firstName(),
                officer.lastName(),
                officer.title()
        );
    }
}

package com.company.officerservice.presentation.controller;

import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OfficerDtoMapper {

    public OfficerFullResponse toFullResponse(OfficerFullView view) {
        return new OfficerFullResponse(
                view.id().value(),
                view.firstName(),
                view.lastName(),
                view.dateOfBirth(),
                view.nationality(),
                toAddressResponse(view.address()),
                view.email(),
                view.phone(),
                toLinkResponses(view.companyLinks()),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public OfficerRestrictedResponse toRestrictedResponse(OfficerRestrictedView view) {
        return new OfficerRestrictedResponse(
                view.id().value(),
                view.firstName(),
                view.lastName(),
                toLinkResponses(view.companyLinks())
        );
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) return null;
        return new AddressResponse(address.street(), address.city(), address.postalCode(), address.country());
    }

    private List<CompanyLinkResponse> toLinkResponses(List<CompanyLink> links) {
        if (links == null) return List.of();
        return links.stream()
                .map(l -> new CompanyLinkResponse(
                        l.companyId(), l.title(), l.appointmentDate(), l.resignationDate(), l.active()))
                .toList();
    }
}

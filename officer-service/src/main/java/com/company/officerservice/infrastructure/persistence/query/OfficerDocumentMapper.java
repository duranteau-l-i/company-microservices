package com.company.officerservice.infrastructure.persistence.query;

import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;

import java.util.List;

public final class OfficerDocumentMapper {

    private OfficerDocumentMapper() {}

    public static OfficerDocument toDocument(OfficerFullView view) {
        List<CompanyLinkDocument> linkDocs = view.companyLinks() == null
                ? List.of()
                : view.companyLinks().stream().map(OfficerDocumentMapper::toLinkDoc).toList();

        String street = view.address() != null ? view.address().street() : null;
        String city = view.address() != null ? view.address().city() : null;
        String postalCode = view.address() != null ? view.address().postalCode() : null;
        String country = view.address() != null ? view.address().country() : null;

        return new OfficerDocument(
                view.id().value(),
                view.firstName(),
                view.lastName(),
                view.dateOfBirth(),
                view.nationality(),
                street, city, postalCode, country,
                view.email(),
                view.phone(),
                linkDocs,
                view.createdAt(),
                view.updatedAt()
        );
    }

    public static OfficerFullView toFullView(OfficerDocument doc) {
        List<CompanyLink> links = doc.getCompanyLinks() == null
                ? List.of()
                : doc.getCompanyLinks().stream().map(OfficerDocumentMapper::toLinkDomain).toList();

        Address address = (doc.getStreet() != null)
                ? new Address(doc.getStreet(), doc.getCity(), doc.getPostalCode(), doc.getCountry())
                : null;

        return new OfficerFullView(
                OfficerId.of(doc.getId()),
                doc.getFirstName(),
                doc.getLastName(),
                doc.getDateOfBirth(),
                doc.getNationality(),
                address,
                doc.getEmail(),
                doc.getPhone(),
                links,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    public static OfficerRestrictedView toRestrictedView(OfficerDocument doc) {
        List<CompanyLink> links = doc.getCompanyLinks() == null
                ? List.of()
                : doc.getCompanyLinks().stream().map(OfficerDocumentMapper::toLinkDomain).toList();

        return new OfficerRestrictedView(
                OfficerId.of(doc.getId()),
                doc.getFirstName(),
                doc.getLastName(),
                links
        );
    }

    private static CompanyLinkDocument toLinkDoc(CompanyLink link) {
        return new CompanyLinkDocument(
                link.companyId(), link.title(),
                link.appointmentDate(), link.resignationDate(), link.active()
        );
    }

    private static CompanyLink toLinkDomain(CompanyLinkDocument doc) {
        return new CompanyLink(
                doc.getCompanyId(), doc.getTitle(),
                doc.getAppointmentDate(), doc.getResignationDate(), doc.isActive()
        );
    }
}

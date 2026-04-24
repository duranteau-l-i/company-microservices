package com.company.companyservice.infrastructure.persistence.query;

import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;

import java.util.List;

public final class CompanyDocumentMapper {

    private CompanyDocumentMapper() {}

    public static CompanyDocument toDocument(CompanyFullView view) {
        List<OfficerSummaryDocument> officerDocs = view.officers() == null
                ? List.of()
                : view.officers().stream().map(CompanyDocumentMapper::toOfficerDoc).toList();

        return new CompanyDocument(
                view.id().value(),
                view.name(),
                view.registrationNumber(),
                view.address().street(),
                view.address().city(),
                view.address().postalCode(),
                view.address().country(),
                view.ownerId(),
                view.ownerDisplayName(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt(),
                officerDocs
        );
    }

    public static CompanyFullView toFullView(CompanyDocument doc) {
        List<OfficerSummary> officers = doc.getOfficers() == null
                ? List.of()
                : doc.getOfficers().stream().map(CompanyDocumentMapper::toOfficerSummary).toList();

        return new CompanyFullView(
                CompanyId.of(doc.getId()),
                doc.getName(),
                doc.getRegistrationNumber(),
                new Address(doc.getStreet(), doc.getCity(), doc.getPostalCode(), doc.getCountry()),
                doc.getOwnerId(),
                doc.getOwnerDisplayName(),
                CompanyStatus.valueOf(doc.getStatus()),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                officers
        );
    }

    public static CompanyRestrictedView toRestrictedView(CompanyDocument doc) {
        return new CompanyRestrictedView(
                CompanyId.of(doc.getId()),
                doc.getName(),
                doc.getRegistrationNumber(),
                doc.getOwnerId(),
                doc.getOwnerDisplayName(),
                CompanyStatus.valueOf(doc.getStatus())
        );
    }

    private static OfficerSummary toOfficerSummary(OfficerSummaryDocument d) {
        return new OfficerSummary(d.getOfficerId(), d.getFirstName(), d.getLastName(), d.getTitle());
    }

    private static OfficerSummaryDocument toOfficerDoc(OfficerSummary s) {
        return new OfficerSummaryDocument(s.officerId(), s.firstName(), s.lastName(), s.title());
    }
}

package com.company.officerservice.infrastructure.persistence.command;

import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerId;

import java.util.List;
import java.util.UUID;

public final class OfficerEntityMapper {

    private OfficerEntityMapper() {}

    public static OfficerEntity toEntity(Officer officer) {
        OfficerEntity entity = new OfficerEntity(
                officer.id().value(),
                officer.firstName(),
                officer.lastName(),
                officer.dateOfBirth(),
                officer.nationality(),
                officer.address().street(),
                officer.address().city(),
                officer.address().postalCode(),
                officer.address().country(),
                officer.email(),
                officer.phone(),
                officer.createdAt(),
                officer.updatedAt()
        );

        List<CompanyLinkEntity> linkEntities = officer.companyLinks().stream()
                .map(link -> toLinkEntity(link, entity))
                .toList();
        entity.setCompanyLinks(linkEntities);

        return entity;
    }

    public static Officer toDomain(OfficerEntity entity) {
        Address address = new Address(
                entity.getStreet(), entity.getCity(),
                entity.getPostalCode(), entity.getCountry()
        );

        List<CompanyLink> links = entity.getCompanyLinks().stream()
                .map(OfficerEntityMapper::toLinkDomain)
                .toList();

        return new Officer(
                OfficerId.of(entity.getId()),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDateOfBirth(),
                entity.getNationality(),
                address,
                entity.getEmail(),
                entity.getPhone(),
                links,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static CompanyLinkEntity toLinkEntity(CompanyLink link, OfficerEntity officer) {
        return new CompanyLinkEntity(
                UUID.randomUUID(),
                officer,
                link.companyId(),
                link.title(),
                link.appointmentDate(),
                link.resignationDate(),
                link.active()
        );
    }

    private static CompanyLink toLinkDomain(CompanyLinkEntity entity) {
        return new CompanyLink(
                entity.getCompanyId(),
                entity.getTitle(),
                entity.getAppointmentDate(),
                entity.getResignationDate(),
                entity.isActive()
        );
    }
}

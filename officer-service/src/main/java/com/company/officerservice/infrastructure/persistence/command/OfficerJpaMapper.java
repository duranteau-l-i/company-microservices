package com.company.officerservice.infrastructure.persistence.command;

import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerId;

import java.util.List;
import java.util.UUID;

public final class OfficerJpaMapper {

    private OfficerJpaMapper() {}

    public static OfficerJpaEntity toEntity(Officer officer) {
        OfficerJpaEntity entity = new OfficerJpaEntity(
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

        List<CompanyLinkJpaEntity> linkEntities = officer.companyLinks().stream()
                .map(link -> toLinkEntity(link, entity))
                .toList();
        entity.setCompanyLinks(linkEntities);

        return entity;
    }

    public static Officer toDomain(OfficerJpaEntity entity) {
        Address address = new Address(
                entity.getStreet(), entity.getCity(),
                entity.getPostalCode(), entity.getCountry()
        );

        List<CompanyLink> links = entity.getCompanyLinks().stream()
                .map(OfficerJpaMapper::toLinkDomain)
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

    private static CompanyLinkJpaEntity toLinkEntity(CompanyLink link, OfficerJpaEntity officer) {
        return new CompanyLinkJpaEntity(
                UUID.randomUUID(),
                officer,
                link.companyId(),
                link.title(),
                link.appointmentDate(),
                link.resignationDate(),
                link.active()
        );
    }

    private static CompanyLink toLinkDomain(CompanyLinkJpaEntity entity) {
        return new CompanyLink(
                entity.getCompanyId(),
                entity.getTitle(),
                entity.getAppointmentDate(),
                entity.getResignationDate(),
                entity.isActive()
        );
    }
}

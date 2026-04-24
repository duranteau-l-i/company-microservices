package com.company.officerservice.unit.domain.model;

import com.company.officerservice.domain.event.OfficerCreatedEvent;
import com.company.officerservice.domain.event.OfficerLinkedToCompanyEvent;
import com.company.officerservice.domain.event.OfficerUnlinkedFromCompanyEvent;
import com.company.officerservice.domain.event.OfficerUpdatedEvent;
import com.company.officerservice.domain.exception.DuplicateLinkException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficerTest {

    private static final Address VALID_ADDRESS = new Address("10 Downing St", "London", "SW1A 2AA", "UK");
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1980, 5, 20);

    private Officer.Created createValidOfficer() {
        return Officer.create("John", "Doe", DATE_OF_BIRTH, "British", VALID_ADDRESS, "john@example.com", "+44123456789");
    }

    @Test
    void createProducesOfficerAndEvent() {
        Officer.Created result = createValidOfficer();

        Officer officer = result.officer();
        assertThat(officer.id()).isNotNull();
        assertThat(officer.firstName()).isEqualTo("John");
        assertThat(officer.lastName()).isEqualTo("Doe");
        assertThat(officer.dateOfBirth()).isEqualTo(DATE_OF_BIRTH);
        assertThat(officer.nationality()).isEqualTo("British");
        assertThat(officer.address()).isEqualTo(VALID_ADDRESS);
        assertThat(officer.email()).isEqualTo("john@example.com");
        assertThat(officer.phone()).isEqualTo("+44123456789");
        assertThat(officer.companyLinks()).isEmpty();
        assertThat(officer.createdAt()).isNotNull();
        assertThat(officer.updatedAt()).isNotNull();

        OfficerCreatedEvent event = result.event();
        assertThat(event.aggregateId()).isEqualTo(officer.id().value());
        assertThat(event.firstName()).isEqualTo("John");
        assertThat(event.lastName()).isEqualTo("Doe");
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("OfficerCreatedEvent");
        assertThat(event.aggregateType()).isEqualTo("Officer");
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatThrownBy(() -> Officer.create("", "Doe", DATE_OF_BIRTH, "British", VALID_ADDRESS, "john@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firstName");
    }

    @Test
    void rejectsBlankLastName() {
        assertThatThrownBy(() -> Officer.create("John", "  ", DATE_OF_BIRTH, "British", VALID_ADDRESS, "john@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastName");
    }

    @Test
    void rejectsNullDateOfBirth() {
        assertThatThrownBy(() -> Officer.create("John", "Doe", null, "British", VALID_ADDRESS, "john@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullAddress() {
        assertThatThrownBy(() -> Officer.create("John", "Doe", DATE_OF_BIRTH, "British", null, "john@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateChangesFieldsAndEmitsEvent() throws InterruptedException {
        Officer officer = createValidOfficer().officer();
        Address newAddress = new Address("20 Baker St", "London", "NW1 6XE", "UK");
        var before = officer.updatedAt();
        Thread.sleep(2);

        Officer.Updated result = officer.update("Jane", "Smith", "French", newAddress, "jane@example.com", null);

        assertThat(officer.firstName()).isEqualTo("Jane");
        assertThat(officer.lastName()).isEqualTo("Smith");
        assertThat(officer.nationality()).isEqualTo("French");
        assertThat(officer.address()).isEqualTo(newAddress);
        assertThat(officer.email()).isEqualTo("jane@example.com");
        assertThat(officer.phone()).isNull();
        assertThat(officer.updatedAt()).isAfter(before);
        assertThat(result.officer()).isSameAs(officer);

        OfficerUpdatedEvent event = result.event();
        assertThat(event).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(officer.id().value());
        assertThat(event.eventType()).isEqualTo("OfficerUpdatedEvent");
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void linkToCompanyAddsLinkAndEmitsEvent() {
        Officer officer = createValidOfficer().officer();
        UUID companyId = UUID.randomUUID();
        CompanyLink link = CompanyLink.create(companyId, "Director", LocalDate.now());

        Officer.Linked result = officer.linkToCompany(link);

        assertThat(officer.companyLinks()).hasSize(1);
        assertThat(officer.companyLinks().get(0).companyId()).isEqualTo(companyId);
        assertThat(result.officer()).isSameAs(officer);

        OfficerLinkedToCompanyEvent event = result.event();
        assertThat(event).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(officer.id().value());
        assertThat(event.companyId()).isEqualTo(companyId);
        assertThat(event.title()).isEqualTo("Director");
        assertThat(event.eventType()).isEqualTo("OfficerLinkedToCompanyEvent");
    }

    @Test
    void linkToCompanyWithDuplicateThrowsDuplicateLinkException() {
        Officer officer = createValidOfficer().officer();
        UUID companyId = UUID.randomUUID();
        CompanyLink link = CompanyLink.create(companyId, "Director", LocalDate.now());
        officer.linkToCompany(link);

        CompanyLink duplicate = CompanyLink.create(companyId, "Director", LocalDate.now());

        assertThatThrownBy(() -> officer.linkToCompany(duplicate))
                .isInstanceOf(DuplicateLinkException.class);
    }

    @Test
    void unlinkFromCompanyResignsLinkAndEmitsEvent() {
        Officer officer = createValidOfficer().officer();
        UUID companyId = UUID.randomUUID();
        CompanyLink link = CompanyLink.create(companyId, "Director", LocalDate.now());
        officer.linkToCompany(link);

        Officer.Unlinked result = officer.unlinkFromCompany(companyId);

        assertThat(officer.companyLinks().get(0).active()).isFalse();
        assertThat(officer.companyLinks().get(0).resignationDate()).isNotNull();
        assertThat(result.officer()).isSameAs(officer);

        OfficerUnlinkedFromCompanyEvent event = result.event();
        assertThat(event).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(officer.id().value());
        assertThat(event.companyId()).isEqualTo(companyId);
        assertThat(event.eventType()).isEqualTo("OfficerUnlinkedFromCompanyEvent");
    }

    @Test
    void unlinkFromCompanyWithUnknownIdThrowsOfficerNotFoundException() {
        Officer officer = createValidOfficer().officer();
        UUID unknownCompanyId = UUID.randomUUID();

        assertThatThrownBy(() -> officer.unlinkFromCompany(unknownCompanyId))
                .isInstanceOf(OfficerNotFoundException.class);
    }
}

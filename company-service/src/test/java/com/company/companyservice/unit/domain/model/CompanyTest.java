package com.company.companyservice.unit.domain.model;

import com.company.companyservice.domain.event.CompanyCreatedEvent;
import com.company.companyservice.domain.event.CompanyUpdatedEvent;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

    private static final Address VALID_ADDRESS = new Address("123 Main St", "Paris", "75001", "France");
    private static final UUID OWNER_ID = UUID.randomUUID();

    @Test
    void createProducesCompanyAndEvent() {
        Company.Created result = Company.create("Acme Corp", "REG-001", VALID_ADDRESS, OWNER_ID);

        Company company = result.company();
        assertThat(company.id()).isNotNull();
        assertThat(company.name()).isEqualTo("Acme Corp");
        assertThat(company.registrationNumber()).isEqualTo("REG-001");
        assertThat(company.address()).isEqualTo(VALID_ADDRESS);
        assertThat(company.ownerId()).isEqualTo(OWNER_ID);
        assertThat(company.status()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(company.createdAt()).isNotNull();
        assertThat(company.updatedAt()).isNotNull();

        CompanyCreatedEvent event = result.event();
        assertThat(event.aggregateId()).isEqualTo(company.id().value());
        assertThat(event.name()).isEqualTo("Acme Corp");
        assertThat(event.registrationNumber()).isEqualTo("REG-001");
        assertThat(event.ownerId()).isEqualTo(OWNER_ID);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("CompanyCreatedEvent");
        assertThat(event.aggregateType()).isEqualTo("Company");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Company.create("", "REG-001", VALID_ADDRESS, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> Company.create(null, "REG-001", VALID_ADDRESS, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsBlankRegistrationNumber() {
        assertThatThrownBy(() -> Company.create("Acme Corp", "   ", VALID_ADDRESS, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registrationNumber");
    }

    @Test
    void rejectsNullRegistrationNumber() {
        assertThatThrownBy(() -> Company.create("Acme Corp", null, VALID_ADDRESS, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registrationNumber");
    }

    @Test
    void rejectsNullAddress() {
        assertThatThrownBy(() -> Company.create("Acme Corp", "REG-001", null, OWNER_ID))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullOwnerId() {
        assertThatThrownBy(() -> Company.create("Acme Corp", "REG-001", VALID_ADDRESS, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deactivateChangesStatusToInactiveAndEmitsEvent() {
        Company company = Company.create("Acme Corp", "REG-001", VALID_ADDRESS, OWNER_ID).company();
        Company.Updated result = company.deactivate();

        assertThat(company.status()).isEqualTo(CompanyStatus.INACTIVE);
        assertThat(result.company()).isSameAs(company);
        CompanyUpdatedEvent event = result.event();
        assertThat(event).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(company.id().value());
        assertThat(event.eventType()).isEqualTo("CompanyUpdatedEvent");
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void activateChangesStatusToActiveAndEmitsEvent() {
        Company company = Company.create("Acme Corp", "REG-001", VALID_ADDRESS, OWNER_ID).company();
        company.deactivate();
        Company.Updated result = company.activate();

        assertThat(company.status()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(result.company()).isSameAs(company);
        CompanyUpdatedEvent event = result.event();
        assertThat(event).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(company.id().value());
        assertThat(event.eventType()).isEqualTo("CompanyUpdatedEvent");
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void deactivateUpdatesTimestamp() throws InterruptedException {
        Company company = Company.create("Acme Corp", "REG-001", VALID_ADDRESS, OWNER_ID).company();
        var before = company.updatedAt();
        Thread.sleep(2);
        company.deactivate();

        assertThat(company.updatedAt()).isAfter(before);
    }

    @Test
    void updateChangesFieldsAndUpdatesTimestamp() throws InterruptedException {
        Company company = Company.create("Acme Corp", "REG-001", VALID_ADDRESS, OWNER_ID).company();
        Address newAddress = new Address("456 New Ave", "Lyon", "69001", "France");
        var before = company.updatedAt();
        Thread.sleep(2);

        Company.Updated result = company.update("New Name", "REG-002", newAddress);

        assertThat(company.name()).isEqualTo("New Name");
        assertThat(company.registrationNumber()).isEqualTo("REG-002");
        assertThat(company.address()).isEqualTo(newAddress);
        assertThat(company.updatedAt()).isAfter(before);
        assertThat(result.company()).isSameAs(company);
        assertThat(result.event()).isNotNull();
        assertThat(result.event().aggregateId()).isEqualTo(company.id().value());
    }

    @Test
    void updateRejectsBlankName() {
        Company company = Company.create("Acme Corp", "REG-001", VALID_ADDRESS, OWNER_ID).company();

        assertThatThrownBy(() -> company.update("", "REG-002", VALID_ADDRESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }
}

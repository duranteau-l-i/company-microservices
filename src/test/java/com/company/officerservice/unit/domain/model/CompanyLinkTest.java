package com.company.officerservice.unit.domain.model;

import com.company.officerservice.domain.model.CompanyLink;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyLinkTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String TITLE = "Director";
    private static final LocalDate APPOINTMENT_DATE = LocalDate.of(2024, 1, 15);

    @Test
    void createsValidLinkWithAllFieldsSet() {
        CompanyLink link = CompanyLink.create(COMPANY_ID, TITLE, APPOINTMENT_DATE);

        assertThat(link.companyId()).isEqualTo(COMPANY_ID);
        assertThat(link.title()).isEqualTo(TITLE);
        assertThat(link.appointmentDate()).isEqualTo(APPOINTMENT_DATE);
    }

    @Test
    void initialStateIsActiveWithNoResignationDate() {
        CompanyLink link = CompanyLink.create(COMPANY_ID, TITLE, APPOINTMENT_DATE);

        assertThat(link.active()).isTrue();
        assertThat(link.resignationDate()).isNull();
    }

    @Test
    void rejectsNullCompanyId() {
        assertThatThrownBy(() -> CompanyLink.create(null, TITLE, APPOINTMENT_DATE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> CompanyLink.create(COMPANY_ID, "  ", APPOINTMENT_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void resignSetsResignationDateAndDeactivates() {
        CompanyLink link = CompanyLink.create(COMPANY_ID, TITLE, APPOINTMENT_DATE);
        LocalDate resignationDate = LocalDate.of(2025, 6, 30);

        link.resign(resignationDate);

        assertThat(link.resignationDate()).isEqualTo(resignationDate);
        assertThat(link.active()).isFalse();
    }
}

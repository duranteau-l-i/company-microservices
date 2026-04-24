package com.company.companyservice.unit.domain.model;

import com.company.companyservice.domain.model.OfficerSummary;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficerSummaryTest {

    private static final UUID OFFICER_ID = UUID.randomUUID();

    @Test
    void validCreation() {
        OfficerSummary summary = new OfficerSummary(OFFICER_ID, "Jane", "Doe", "Director");

        assertThat(summary.officerId()).isEqualTo(OFFICER_ID);
        assertThat(summary.firstName()).isEqualTo("Jane");
        assertThat(summary.lastName()).isEqualTo("Doe");
        assertThat(summary.title()).isEqualTo("Director");
    }

    @Test
    void rejectsNullOfficerId() {
        assertThatThrownBy(() -> new OfficerSummary(null, "Jane", "Doe", "Director"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("officerId");
    }

    @Test
    void rejectsNullFirstName() {
        assertThatThrownBy(() -> new OfficerSummary(OFFICER_ID, null, "Doe", "Director"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firstName");
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatThrownBy(() -> new OfficerSummary(OFFICER_ID, "   ", "Doe", "Director"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firstName");
    }

    @Test
    void rejectsNullLastName() {
        assertThatThrownBy(() -> new OfficerSummary(OFFICER_ID, "Jane", null, "Director"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastName");
    }

    @Test
    void rejectsBlankLastName() {
        assertThatThrownBy(() -> new OfficerSummary(OFFICER_ID, "Jane", "   ", "Director"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastName");
    }

    @Test
    void rejectsNullTitle() {
        assertThatThrownBy(() -> new OfficerSummary(OFFICER_ID, "Jane", "Doe", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> new OfficerSummary(OFFICER_ID, "Jane", "Doe", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }
}

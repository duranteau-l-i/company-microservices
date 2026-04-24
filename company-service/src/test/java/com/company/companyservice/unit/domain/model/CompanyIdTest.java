package com.company.companyservice.unit.domain.model;

import com.company.companyservice.domain.model.CompanyId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyIdTest {

    @Test
    void generatesRandomId() {
        CompanyId id1 = CompanyId.generate();
        CompanyId id2 = CompanyId.generate();

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotNull();
    }

    @Test
    void wrapsProvidedUuid() {
        UUID uuid = UUID.randomUUID();
        CompanyId id = CompanyId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new CompanyId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isEqualByValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(CompanyId.of(uuid)).isEqualTo(CompanyId.of(uuid));
    }

    @Test
    void fromStringParsesUuid() {
        UUID uuid = UUID.randomUUID();
        CompanyId id = CompanyId.fromString(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toStringReturnsUuidString() {
        UUID uuid = UUID.randomUUID();
        CompanyId id = CompanyId.of(uuid);

        assertThat(id.toString()).isEqualTo(uuid.toString());
    }
}

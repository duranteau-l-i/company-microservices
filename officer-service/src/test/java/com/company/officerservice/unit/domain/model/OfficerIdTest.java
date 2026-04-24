package com.company.officerservice.unit.domain.model;

import com.company.officerservice.domain.model.OfficerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficerIdTest {

    @Test
    void generatesRandomId() {
        OfficerId id1 = OfficerId.generate();
        OfficerId id2 = OfficerId.generate();

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotNull();
    }

    @Test
    void wrapsProvidedUuid() {
        UUID uuid = UUID.randomUUID();
        OfficerId id = OfficerId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new OfficerId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isEqualByValue() {
        UUID uuid = UUID.randomUUID();

        assertThat(OfficerId.of(uuid)).isEqualTo(OfficerId.of(uuid));
    }

    @Test
    void fromStringParsesUuid() {
        UUID uuid = UUID.randomUUID();
        OfficerId id = OfficerId.fromString(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toStringReturnsUuidString() {
        UUID uuid = UUID.randomUUID();
        OfficerId id = OfficerId.of(uuid);

        assertThat(id.toString()).isEqualTo(uuid.toString());
    }
}

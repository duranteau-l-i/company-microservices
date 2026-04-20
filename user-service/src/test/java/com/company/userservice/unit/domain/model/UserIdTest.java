package com.company.userservice.unit.domain.model;

import com.company.userservice.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void generatesRandomId() {
        UserId id1 = UserId.generate();
        UserId id2 = UserId.generate();

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotNull();
    }

    @Test
    void wrapsProvidedUuid() {
        UUID uuid = UUID.randomUUID();
        UserId id = UserId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isEqualByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(UserId.of(uuid)).isEqualTo(UserId.of(uuid));
    }
}
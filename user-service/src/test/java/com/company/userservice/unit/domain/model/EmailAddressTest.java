package com.company.userservice.unit.domain.model;

import com.company.userservice.domain.model.EmailAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void acceptsValidEmail() {
        EmailAddress email = EmailAddress.of("foo@bar.com");
        assertThat(email.value()).isEqualTo("foo@bar.com");
    }

    @Test
    void normalizesToLowerCase() {
        EmailAddress email = EmailAddress.of("Foo@Bar.com");
        assertThat(email.value()).isEqualTo("foo@bar.com");
    }

    @Test
    void trimsWhitespace() {
        EmailAddress email = EmailAddress.of("  user@domain.com  ");
        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @Test
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> EmailAddress.of("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> EmailAddress.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> EmailAddress.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isEqualByNormalizedValue() {
        assertThat(EmailAddress.of("A@B.COM")).isEqualTo(EmailAddress.of("a@b.com"));
    }
}

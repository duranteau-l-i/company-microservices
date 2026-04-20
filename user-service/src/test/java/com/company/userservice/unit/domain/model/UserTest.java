package com.company.userservice.unit.domain.model;

import com.company.userservice.domain.event.UserCreatedEvent;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void createProducesUserAndEvent() {
        User.Created result = User.create(
                EmailAddress.of("jane@doe.com"),
                "hashed",
                "Jane",
                "Doe",
                Role.USER);

        assertThat(result.user().id()).isNotNull();
        assertThat(result.user().email().value()).isEqualTo("jane@doe.com");
        assertThat(result.user().role()).isEqualTo(Role.USER);
        assertThat(result.user().active()).isTrue();

        UserCreatedEvent event = result.event();
        assertThat(event.aggregateId()).isEqualTo(result.user().id().value());
        assertThat(event.email()).isEqualTo("jane@doe.com");
        assertThat(event.role()).isEqualTo("USER");
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatThrownBy(() -> User.create(
                EmailAddress.of("a@b.com"), "h", "", "Doe", Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> User.create(
                EmailAddress.of("a@b.com"), "", "Jane", "Doe", Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateProfileUpdatesNamesAndTimestamp() throws InterruptedException {
        User user = User.create(EmailAddress.of("a@b.com"), "h", "Jane", "Doe", Role.USER).user();
        var before = user.updatedAt();
        Thread.sleep(2);
        user.updateProfile("Janet", "Smith");

        assertThat(user.firstName()).isEqualTo("Janet");
        assertThat(user.lastName()).isEqualTo("Smith");
        assertThat(user.updatedAt()).isAfter(before);
    }

    @Test
    void deactivateMarksInactive() {
        User user = User.create(EmailAddress.of("a@b.com"), "h", "Jane", "Doe", Role.USER).user();
        user.deactivate();
        assertThat(user.active()).isFalse();
    }
}

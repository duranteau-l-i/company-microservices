package com.company.userservice.unit.application.command;

import com.company.userservice.application.command.UpdateUserHandler;
import com.company.userservice.domain.event.UserUpdatedEvent;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.UpdateUserUseCase;
import com.company.userservice.stubs.InMemoryUserCommandRepository;
import com.company.userservice.stubs.InMemoryUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateUserHandlerTest {

    private InMemoryUserCommandRepository repo;
    private InMemoryUserEventPublisher publisher;
    private UpdateUserHandler handler;
    private User existing;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserCommandRepository();
        publisher = new InMemoryUserEventPublisher();
        handler = new UpdateUserHandler(repo, publisher);
        existing = User.create(EmailAddress.of("una@test.com"), "h", "Una", "User", Role.USER).user();
        repo.save(existing);
    }

    @Test
    void userUpdatesOwnProfile() {
        UserReadModel result = handler.update(new UpdateUserUseCase.Command(
                existing.id(), Role.USER, existing.id(), "Unity", "Updated"));

        assertThat(result.firstName()).isEqualTo("Unity");
        assertThat(result.lastName()).isEqualTo("Updated");
        assertThat(publisher.lastEvent()).isInstanceOf(UserUpdatedEvent.class);
    }

    @Test
    void userCannotUpdateAnother() {
        User other = User.create(EmailAddress.of("other@test.com"), "h", "Other", "User", Role.USER).user();
        repo.save(other);

        assertThatThrownBy(() -> handler.update(new UpdateUserUseCase.Command(
                existing.id(), Role.USER, other.id(), "Hacked", "Name")))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void adminUpdatesAnyone() {
        User other = User.create(EmailAddress.of("other@test.com"), "h", "Other", "User", Role.USER).user();
        repo.save(other);
        User admin = User.create(EmailAddress.of("admin@test.com"), "h", "A", "A", Role.ADMIN).user();
        repo.save(admin);

        UserReadModel result = handler.update(new UpdateUserUseCase.Command(
                admin.id(), Role.ADMIN, other.id(), "Renamed", "ByAdmin"));

        assertThat(result.firstName()).isEqualTo("Renamed");
    }
}

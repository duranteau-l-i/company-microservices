package com.company.userservice.unit.application.command;

import com.company.userservice.application.command.CreateUserHandler;
import com.company.userservice.domain.exception.DuplicateEmailException;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.CreateUserUseCase;
import com.company.userservice.unit.application.stubs.InMemoryPasswordHasher;
import com.company.userservice.unit.application.stubs.InMemoryUserCommandRepository;
import com.company.userservice.unit.application.stubs.InMemoryUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateUserHandlerTest {

    private InMemoryUserCommandRepository repo;
    private InMemoryUserEventPublisher publisher;
    private CreateUserHandler handler;
    private final UserId caller = UserId.generate();

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserCommandRepository();
        publisher = new InMemoryUserEventPublisher();
        handler = new CreateUserHandler(repo, publisher, new InMemoryPasswordHasher());
    }

    @Test
    void adminCreatesManager() {
        UserReadModel result = handler.create(new CreateUserUseCase.Command(
                caller, Role.ADMIN, "mary@test.com", "pw123456", "Mary", "Manager", Role.MANAGER));

        assertThat(result.role()).isEqualTo(Role.MANAGER);
        assertThat(repo.size()).isEqualTo(1);
    }

    @Test
    void managerCreatesUser() {
        UserReadModel result = handler.create(new CreateUserUseCase.Command(
                caller, Role.MANAGER, "una@test.com", "pw123456", "Una", "User", Role.USER));

        assertThat(result.role()).isEqualTo(Role.USER);
    }

    @Test
    void managerCannotCreateManager() {
        assertThatThrownBy(() -> handler.create(new CreateUserUseCase.Command(
                caller, Role.MANAGER, "mary@test.com", "pw123456", "Mary", "Manager", Role.MANAGER)))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void userCannotCreateUser() {
        assertThatThrownBy(() -> handler.create(new CreateUserUseCase.Command(
                caller, Role.USER, "una@test.com", "pw123456", "Una", "User", Role.USER)))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void rejectsDuplicateEmail() {
        handler.create(new CreateUserUseCase.Command(
                caller, Role.ADMIN, "duplicate@test.com", "pw123456", "A", "B", Role.MANAGER));

        assertThatThrownBy(() -> handler.create(new CreateUserUseCase.Command(
                caller, Role.ADMIN, "duplicate@test.com", "pw123456", "C", "D", Role.USER)))
                .isInstanceOf(DuplicateEmailException.class);
    }
}

package com.company.userservice.unit.application.command;

import com.company.userservice.application.command.DeleteUserHandler;
import com.company.userservice.domain.event.UserDeletedEvent;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.port.usecases.DeleteUserUseCase;
import com.company.userservice.stubs.InMemoryUserCommandRepository;
import com.company.userservice.stubs.InMemoryUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteUserHandlerTest {

    private InMemoryUserCommandRepository repo;
    private InMemoryUserEventPublisher publisher;
    private DeleteUserHandler handler;
    private User target;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserCommandRepository();
        publisher = new InMemoryUserEventPublisher();
        handler = new DeleteUserHandler(repo, publisher);
        target = User.create(EmailAddress.of("terry@test.com"), "h", "T", "Target", Role.USER).user();
        repo.save(target);
    }

    @Test
    void adminDeletesUserSoftDelete() {
        handler.delete(new DeleteUserUseCase.Command(target.id(), Role.ADMIN, target.id()));

        assertThat(repo.findById(target.id())).isPresent();
        assertThat(repo.findById(target.id()).get().active()).isFalse();
        assertThat(publisher.lastEvent()).isInstanceOf(UserDeletedEvent.class);
    }

    @Test
    void managerCannotDelete() {
        assertThatThrownBy(() -> handler.delete(new DeleteUserUseCase.Command(
                target.id(), Role.MANAGER, target.id())))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void userCannotDelete() {
        assertThatThrownBy(() -> handler.delete(new DeleteUserUseCase.Command(
                target.id(), Role.USER, target.id())))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}

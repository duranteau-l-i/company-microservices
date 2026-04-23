package com.company.userservice.unit.application.command;

import com.company.userservice.application.command.SignUpHandler;
import com.company.userservice.domain.event.UserCreatedEvent;
import com.company.userservice.domain.exception.DuplicateEmailException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.SignUpUseCase;
import com.company.userservice.stubs.InMemoryPasswordHasher;
import com.company.userservice.stubs.InMemoryUserCommandRepository;
import com.company.userservice.stubs.InMemoryUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignUpHandlerTest {

    private InMemoryUserCommandRepository repo;
    private InMemoryUserEventPublisher publisher;
    private SignUpHandler handler;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserCommandRepository();
        publisher = new InMemoryUserEventPublisher();
        handler = new SignUpHandler(repo, publisher, new InMemoryPasswordHasher());
    }

    @Test
    void signUpCreatesUserAndPublishesEvent() {
        UserReadModel result = handler.signUp(new SignUpUseCase.Command(
                "jane@doe.com", "pw123456", "Jane", "Doe"));

        assertThat(result.email().value()).isEqualTo("jane@doe.com");
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(repo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(UserCreatedEvent.class);
    }

    @Test
    void signUpNormalizesEmailBeforeStoring() {
        handler.signUp(new SignUpUseCase.Command("JANE@DOE.COM", "pw123456", "Jane", "Doe"));

        assertThatThrownBy(() -> handler.signUp(new SignUpUseCase.Command(
                "jane@doe.com", "pw123456", "Jane", "Doe")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void signUpRejectsDuplicateEmail() {
        handler.signUp(new SignUpUseCase.Command("jane@doe.com", "pw123456", "Jane", "Doe"));

        assertThatThrownBy(() -> handler.signUp(new SignUpUseCase.Command(
                "jane@doe.com", "pw123456", "Other", "Person")))
                .isInstanceOf(DuplicateEmailException.class);
    }
}

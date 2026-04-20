package com.company.userservice.unit.application.command;

import com.company.userservice.application.command.SignInHandler;
import com.company.userservice.application.command.SignUpHandler;
import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.port.in.SignInUseCase;
import com.company.userservice.domain.port.in.SignUpUseCase;
import com.company.userservice.domain.port.out.TokenProvider.TokenPair;
import com.company.userservice.unit.application.inmemory.InMemoryPasswordHasher;
import com.company.userservice.unit.application.inmemory.InMemoryTokenProvider;
import com.company.userservice.unit.application.inmemory.InMemoryUserCommandRepository;
import com.company.userservice.unit.application.inmemory.InMemoryUserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignInHandlerTest {

    private InMemoryUserCommandRepository repo;
    private InMemoryPasswordHasher hasher;
    private InMemoryTokenProvider tokens;
    private SignInHandler handler;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserCommandRepository();
        hasher = new InMemoryPasswordHasher();
        tokens = new InMemoryTokenProvider();
        handler = new SignInHandler(repo, hasher, tokens);

        new SignUpHandler(repo, new InMemoryUserEventPublisher(), hasher)
                .signUp(new SignUpUseCase.Command("jane@doe.com", "secret", "Jane", "Doe"));
    }

    @Test
    void signInReturnsTokensWithValidCredentials() {
        TokenPair pair = handler.signIn(new SignInUseCase.Command("jane@doe.com", "secret"));

        assertThat(pair.accessToken()).startsWith("access-");
        assertThat(pair.refreshToken()).startsWith("refresh-");
        assertThat(pair.expiresInSeconds()).isEqualTo(1800L);
    }

    @Test
    void signInRejectsUnknownEmail() {
        assertThatThrownBy(() -> handler.signIn(new SignInUseCase.Command("who@doe.com", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void signInRejectsWrongPassword() {
        assertThatThrownBy(() -> handler.signIn(new SignInUseCase.Command("jane@doe.com", "bad")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void signInRejectsMalformedEmail() {
        assertThatThrownBy(() -> handler.signIn(new SignInUseCase.Command("not-an-email", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

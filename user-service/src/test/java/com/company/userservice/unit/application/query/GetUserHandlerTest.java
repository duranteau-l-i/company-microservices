package com.company.userservice.unit.application.query;

import com.company.userservice.application.query.GetUserHandler;
import com.company.userservice.domain.exception.UserNotFoundException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.in.GetUserUseCase;
import com.company.userservice.unit.application.inmemory.InMemoryUserQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetUserHandlerTest {

    private InMemoryUserQueryRepository repo;
    private GetUserHandler handler;
    private User user;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserQueryRepository();
        handler = new GetUserHandler(repo);
        user = User.create(EmailAddress.of("u@co.com"), "h", "U", "User", Role.USER).user();
        repo.save(UserReadModel.from(user));
    }

    @Test
    void returnsUser() {
        UserReadModel result = handler.get(new GetUserUseCase.Query(user.id(), Role.USER, user.id()));

        assertThat(result.email().value()).isEqualTo("u@co.com");
    }

    @Test
    void throwsWhenMissing() {
        UserId missing = UserId.generate();

        assertThatThrownBy(() -> handler.get(new GetUserUseCase.Query(user.id(), Role.USER, missing)))
                .isInstanceOf(UserNotFoundException.class);
    }
}

package com.company.userservice.unit.application.query;

import com.company.userservice.application.query.GetUserHandler;
import com.company.userservice.domain.exception.UserNotFoundException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.GetUserUseCase;
import com.company.userservice.stubs.InMemoryUserQueryRepository;
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
        user = User.create(EmailAddress.of("u@test.com"), "h", "U", "User", Role.USER).user();
        repo.save(UserReadModel.from(user));
    }

    @Test
    void returnsUser() {
        UserReadModel result = handler.get(new GetUserUseCase.Query(user.id(), Role.USER, user.id()));

        assertThat(result.email().value()).isEqualTo("u@test.com");
    }

    @Test
    void throwsWhenMissing() {
        UserId missing = UserId.generate();

        assertThatThrownBy(() -> handler.get(new GetUserUseCase.Query(user.id(), Role.USER, missing)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void userCannotFetchAnotherUsersProfile() {
        UserId otherUserId = UserId.generate();

        assertThatThrownBy(() -> handler.get(new GetUserUseCase.Query(otherUserId, Role.USER, user.id())))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void userCanFetchOwnProfile() {
        UserReadModel result = handler.get(new GetUserUseCase.Query(user.id(), Role.USER, user.id()));

        assertThat(result.id()).isEqualTo(user.id());
    }

    @Test
    void managerCanFetchAnyProfile() {
        UserId managerId = UserId.generate();

        UserReadModel result = handler.get(new GetUserUseCase.Query(managerId, Role.MANAGER, user.id()));

        assertThat(result.id()).isEqualTo(user.id());
    }

    @Test
    void adminCanFetchAnyProfile() {
        UserId adminId = UserId.generate();

        UserReadModel result = handler.get(new GetUserUseCase.Query(adminId, Role.ADMIN, user.id()));

        assertThat(result.id()).isEqualTo(user.id());
    }
}

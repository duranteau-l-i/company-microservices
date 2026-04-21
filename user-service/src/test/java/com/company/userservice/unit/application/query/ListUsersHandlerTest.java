package com.company.userservice.unit.application.query;

import com.company.userservice.application.query.ListUsersHandler;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.ListUsersUseCase;
import com.company.userservice.unit.application.stubs.InMemoryUserQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListUsersHandlerTest {

    private InMemoryUserQueryRepository repo;
    private ListUsersHandler handler;

    @BeforeEach
    void setUp() {
        repo = new InMemoryUserQueryRepository();
        handler = new ListUsersHandler(repo);
        repo.save(UserReadModel.from(User.create(EmailAddress.of("jane@test.com"), "h", "Jane", "Doe", Role.USER).user()));
        repo.save(UserReadModel.from(User.create(EmailAddress.of("bob@test.com"), "h", "Bob", "Smith", Role.MANAGER).user()));
    }

    @Test
    void adminListsAll() {
        List<UserReadModel> result = handler.list(new ListUsersUseCase.Query(UserId.generate(), Role.ADMIN, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void managerListsAll() {
        List<UserReadModel> result = handler.list(new ListUsersUseCase.Query(UserId.generate(), Role.MANAGER, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void userCannotList() {
        assertThatThrownBy(() -> handler.list(new ListUsersUseCase.Query(UserId.generate(), Role.USER, null)))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void searchFiltersByName() {
        List<UserReadModel> result = handler.list(new ListUsersUseCase.Query(UserId.generate(), Role.ADMIN, "Jane"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email().value()).isEqualTo("jane@test.com");
    }
}

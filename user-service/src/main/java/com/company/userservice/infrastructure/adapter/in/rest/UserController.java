package com.company.userservice.infrastructure.adapter.in.rest;

import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.in.CreateUserUseCase;
import com.company.userservice.domain.port.in.DeleteUserUseCase;
import com.company.userservice.domain.port.in.GetUserUseCase;
import com.company.userservice.domain.port.in.ListUsersUseCase;
import com.company.userservice.domain.port.in.UpdateUserUseCase;
import com.company.userservice.infrastructure.adapter.in.rest.dto.CreateUserRequest;
import com.company.userservice.infrastructure.adapter.in.rest.dto.UpdateUserRequest;
import com.company.userservice.infrastructure.adapter.in.rest.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints (authentication required)")
public class UserController {

    private final CreateUserUseCase createUser;
    private final UpdateUserUseCase updateUser;
    private final DeleteUserUseCase deleteUser;
    private final GetUserUseCase getUser;
    private final ListUsersUseCase listUsers;

    public UserController(
            CreateUserUseCase createUser,
            UpdateUserUseCase updateUser,
            DeleteUserUseCase deleteUser,
            GetUserUseCase getUser,
            ListUsersUseCase listUsers) {
        this.createUser = createUser;
        this.updateUser = updateUser;
        this.deleteUser = deleteUser;
        this.getUser = getUser;
        this.listUsers = listUsers;
    }

    @Operation(summary = "Create a user with a given role (ADMIN only)")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        AuthenticatedCaller caller = AuthenticatedCaller.current();

        UserResponse response = UserResponse.from(createUser.create(new CreateUserUseCase.Command(
                caller.id(),
                caller.role(),
                req.email(),
                req.password(),
                req.firstName(),
                req.lastName(),
                req.role())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Fetch a user by id (callers can read themselves; ADMIN/MANAGER can read anyone)")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        AuthenticatedCaller caller = AuthenticatedCaller.current();

        return UserResponse.from(getUser.get(
                new GetUserUseCase.Query(caller.id(), caller.role(), UserId.of(id))));
    }

    @Operation(summary = "List users, optionally filtered by a free-text search (ADMIN/MANAGER only)")
    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) String search) {
        AuthenticatedCaller caller = AuthenticatedCaller.current();

        return listUsers.list(new ListUsersUseCase.Query(caller.id(), caller.role(), search)).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Operation(summary = "Update a user's profile (callers can update themselves; ADMIN can update anyone)")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        AuthenticatedCaller caller = AuthenticatedCaller.current();

        return UserResponse.from(updateUser.update(new UpdateUserUseCase.Command(
                caller.id(),
                caller.role(),
                UserId.of(id),
                req.firstName(),
                req.lastName())));
    }

    @Operation(summary = "Delete a user (ADMIN only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        AuthenticatedCaller caller = AuthenticatedCaller.current();

        deleteUser.delete(new DeleteUserUseCase.Command(caller.id(), caller.role(), UserId.of(id)));
        
        return ResponseEntity.noContent().build();
    }
}

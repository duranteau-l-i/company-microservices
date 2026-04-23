package com.company.userservice.integration.rest;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends BaseRestIT {

    private void seedReadModel(UserId id, String email, Role role) {
        queryRepo.save(new UserReadModel(
                id, EmailAddress.of(email), "First", "Last", role, true, Instant.now(), Instant.now()));
    }

    private void seedWriteModel(UserId id, String email, Role role) {
        commandRepo.save(new User(
                id, EmailAddress.of(email), passwordHasher.hash("pass1234"),
                "First", "Last", role, true, Instant.now(), Instant.now()));
    }

    // --- GET /api/users/{id} ---

    @Test
    void getById_self_returnsUser() throws Exception {
        UserId id = UserId.generate();
        String email = "get-self-" + UUID.randomUUID() + "@test.com";
        seedReadModel(id, email, Role.USER);

        mockMvc.perform(get("/api/users/" + id.value())
                .header("Authorization", token(id, email, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void getById_asAdmin_returnsAnyUser() throws Exception {
        UserId adminId = UserId.generate();
        UserId targetId = UserId.generate();
        String email = "get-admin-" + UUID.randomUUID() + "@test.com";
        seedReadModel(targetId, email, Role.USER);

        mockMvc.perform(get("/api/users/" + targetId.value())
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId.value().toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UserId adminId = UserId.generate();

        mockMvc.perform(get("/api/users/" + UUID.randomUUID())
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/users ---

    @Test
    void list_asAdmin_returnsArray() throws Exception {
        UserId adminId = UserId.generate();
        seedReadModel(UserId.generate(), "list-" + UUID.randomUUID() + "@test.com", Role.USER);

        mockMvc.perform(get("/api/users")
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void list_withSearch_filtersResults() throws Exception {
        UserId adminId = UserId.generate();
        String unique = UUID.randomUUID().toString().replace("-", "");
        String email = unique + "@test.com";
        seedReadModel(UserId.generate(), email, Role.USER);

        mockMvc.perform(get("/api/users").param("search", unique)
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(email));
    }

    @Test
    void list_asUser_returnsForbidden() throws Exception {
        UserId userId = UserId.generate();

        mockMvc.perform(get("/api/users")
                .header("Authorization", token(userId, "user@test.com", Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // --- POST /api/users ---

    @Test
    void create_asAdmin_returnsCreatedUser() throws Exception {
        UserId adminId = UserId.generate();
        String email = "create-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/api/users")
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email, "password", "password123",
                        "firstName", "New", "lastName", "User", "role", "USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void create_asUser_returnsForbidden() throws Exception {
        UserId userId = UserId.generate();

        mockMvc.perform(post("/api/users")
                .header("Authorization", token(userId, "user@test.com", Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "blocked-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "firstName", "Alice", "lastName", "Smith", "role", "USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "noauth-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "firstName", "A", "lastName", "B", "role", "USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withMissingRole_returnsBadRequest() throws Exception {
        UserId adminId = UserId.generate();

        mockMvc.perform(post("/api/users")
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "norole-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "firstName", "Alice", "lastName", "Smith"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withShortFirstName_returnsBadRequest() throws Exception {
        UserId adminId = UserId.generate();

        mockMvc.perform(post("/api/users")
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "short-fn-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "firstName", "Al", "lastName", "Smith", "role", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withShortLastName_returnsBadRequest() throws Exception {
        UserId adminId = UserId.generate();

        mockMvc.perform(post("/api/users")
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "short-ln-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "firstName", "Alice", "lastName", "Li", "role", "USER"))))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/users/{id} ---

    @Test
    void update_self_returnsUpdatedUser() throws Exception {
        UserId id = UserId.generate();
        String email = "update-self-" + UUID.randomUUID() + "@test.com";
        seedWriteModel(id, email, Role.USER);

        mockMvc.perform(put("/api/users/" + id.value())
                .header("Authorization", token(id, email, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("firstName", "Updated", "lastName", "Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    void update_asAdmin_returnsUpdatedUser() throws Exception {
        UserId adminId = UserId.generate();
        UserId targetId = UserId.generate();
        String email = "update-admin-" + UUID.randomUUID() + "@test.com";
        seedWriteModel(targetId, email, Role.USER);

        mockMvc.perform(put("/api/users/" + targetId.value())
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("firstName", "AdminSet", "lastName", "Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("AdminSet"));
    }

    @Test
    void update_otherUserAsUser_returnsForbidden() throws Exception {
        UserId callerId = UserId.generate();
        UserId targetId = UserId.generate();
        String email = "update-other-" + UUID.randomUUID() + "@test.com";
        seedWriteModel(targetId, email, Role.USER);

        mockMvc.perform(put("/api/users/" + targetId.value())
                .header("Authorization", token(callerId, "caller@test.com", Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("firstName", "Hacked", "lastName", "Name"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/users/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("firstName", "A", "lastName", "B"))))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/users/{id} ---

    @Test
    void delete_asAdmin_returnsNoContent() throws Exception {
        UserId adminId = UserId.generate();
        UserId targetId = UserId.generate();
        String email = "delete-" + UUID.randomUUID() + "@test.com";
        seedWriteModel(targetId, email, Role.USER);

        mockMvc.perform(delete("/api/users/" + targetId.value())
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asUser_returnsForbidden() throws Exception {
        UserId userId = UserId.generate();

        mockMvc.perform(delete("/api/users/" + UUID.randomUUID())
                .header("Authorization", token(userId, "user@test.com", Role.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        UserId adminId = UserId.generate();

        mockMvc.perform(delete("/api/users/" + UUID.randomUUID())
                .header("Authorization", token(adminId, "admin@test.com", Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_withoutToken_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}

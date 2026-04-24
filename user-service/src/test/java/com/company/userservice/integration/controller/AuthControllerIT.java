package com.company.userservice.integration.controller;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends BaseRestIT {

    @Test
    void signup_returnsCreatedUser() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "signup-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "firstName", "Alice", "lastName", "Smith")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void signup_withInvalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "not-an-email", "password", "password123",
                        "firstName", "A", "lastName", "B")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_withShortPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "short-" + UUID.randomUUID() + "@test.com",
                        "password", "short", "firstName", "A", "lastName", "B")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_withMissingFirstName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "missing-" + UUID.randomUUID() + "@test.com",
                        "password", "password123", "lastName", "B")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_withDuplicateEmail_returnsConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.com";
        String body = json("email", email, "password", "password123", "firstName", "A", "lastName", "B");

        mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void signin_returnsTokenPair() throws Exception {
        String email = "signin-" + UUID.randomUUID() + "@test.com";
        commandRepo.save(User.create(
                EmailAddress.of(email), passwordHasher.hash("pass1234"), "F", "L", Role.USER).user());

        mockMvc.perform(post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", "pass1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void signin_withWrongPassword_returnsUnauthorized() throws Exception {
        String email = "wrong-pw-" + UUID.randomUUID() + "@test.com";
        commandRepo.save(User.create(
                EmailAddress.of(email), passwordHasher.hash("correct"), "F", "L", Role.USER).user());

        mockMvc.perform(post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signin_withUnknownEmail_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "nobody-" + UUID.randomUUID() + "@test.com", "password", "pass1234")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returnsNewTokenPair() throws Exception {
        String email = "refresh-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", "password123", "firstName", "R", "lastName", "E")))
                .andExpect(status().isCreated());

        String signinBody = mockMvc.perform(post("/api/users/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", "password123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(signinBody).get("refreshToken").asText();

        mockMvc.perform(post("/api/users/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("refreshToken", "not.a.valid.jwt")))
                .andExpect(status().isUnauthorized());
    }

    private String json(String... keyValues) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }
}

package com.company.userservice.presentation.controller.dto;

import com.company.userservice.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 200) String password,
        @NotBlank @Size(min = 3) String firstName,
        @NotBlank @Size(min = 3) String lastName,
        @NotNull Role role) {}

package com.company.userservice.presentation.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String firstName,
        @NotBlank String lastName) {}

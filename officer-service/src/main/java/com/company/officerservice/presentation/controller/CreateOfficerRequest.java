package com.company.officerservice.presentation.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateOfficerRequest(
        @NotNull UUID companyId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull LocalDate dateOfBirth,
        @NotBlank String nationality,
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String postalCode,
        @NotBlank String country,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String title,
        @NotNull LocalDate appointmentDate
) {}

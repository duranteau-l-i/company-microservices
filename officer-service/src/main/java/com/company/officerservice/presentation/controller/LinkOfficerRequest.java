package com.company.officerservice.presentation.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record LinkOfficerRequest(
        @NotNull UUID companyId,
        @NotNull UUID companyOwnerId,
        @NotBlank String title,
        @NotNull LocalDate appointmentDate
) {}

package com.company.companyservice.presentation.controller;

import jakarta.validation.constraints.NotBlank;

public record UpdateCompanyRequest(
        @NotBlank String name,
        @NotBlank String registrationNumber,
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String postalCode,
        @NotBlank String country
) {
}

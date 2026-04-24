package com.company.officerservice.presentation.controller;

import java.time.LocalDate;
import java.util.UUID;

public record CompanyLinkResponse(
        UUID companyId,
        String title,
        LocalDate appointmentDate,
        LocalDate resignationDate,
        boolean active
) {}

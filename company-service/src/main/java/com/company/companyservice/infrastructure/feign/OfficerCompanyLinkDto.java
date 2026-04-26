package com.company.companyservice.infrastructure.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OfficerCompanyLinkDto(UUID companyId, String title) {}

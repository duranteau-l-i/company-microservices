package com.company.officerservice.infrastructure.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompanyClientDto(UUID id) {
}

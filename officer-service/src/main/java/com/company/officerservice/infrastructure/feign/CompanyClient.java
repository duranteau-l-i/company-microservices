package com.company.officerservice.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", fallbackFactory = CompanyClientFallbackFactory.class)
public interface CompanyClient {

    @GetMapping("/api/companies/{id}")
    CompanyClientDto getCompany(@PathVariable UUID id);
}

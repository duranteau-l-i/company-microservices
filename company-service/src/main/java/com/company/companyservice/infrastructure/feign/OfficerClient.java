package com.company.companyservice.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "officer-service", fallbackFactory = OfficerClientFallbackFactory.class)
public interface OfficerClient {

    @GetMapping("/api/officers/by-company/{companyId}")
    List<OfficerClientDto> getOfficersByCompanyId(@PathVariable UUID companyId);
}

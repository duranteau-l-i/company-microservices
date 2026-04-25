package com.company.officerservice.infrastructure.feign;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CompanyClientFallbackFactory implements FallbackFactory<CompanyClient> {

    private static final Logger log = LoggerFactory.getLogger(CompanyClientFallbackFactory.class);

    @Override
    public CompanyClient create(Throwable cause) {
        return id -> {
            if (cause instanceof feign.FeignException.NotFound notFound) {
                throw notFound;
            }
            log.warn("company-service unavailable for companyId={}: {}", id, cause.getMessage());
            throw new ServiceUnavailableException("Cannot verify company — try again later");
        };
    }
}

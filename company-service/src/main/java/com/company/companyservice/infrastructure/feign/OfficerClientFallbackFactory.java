package com.company.companyservice.infrastructure.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OfficerClientFallbackFactory implements FallbackFactory<OfficerClient> {

    private static final Logger log = LoggerFactory.getLogger(OfficerClientFallbackFactory.class);

    static final ThreadLocal<Boolean> FALLBACK_FIRED = new ThreadLocal<>();

    @Override
    public OfficerClient create(Throwable cause) {
        return companyId -> {
            FALLBACK_FIRED.set(true);
            log.warn("officer-service unavailable for companyId={}: {}", companyId, cause.getMessage());
            return List.of();
        };
    }
}

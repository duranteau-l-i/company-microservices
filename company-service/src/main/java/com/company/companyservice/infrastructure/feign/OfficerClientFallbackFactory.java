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

    @Override
    public OfficerClient create(Throwable cause) {
        log.warn("officer-service call failed, activating fallback. Cause: {}", cause.getMessage());
        return new FallbackOfficerClient();
    }

    private static class FallbackOfficerClient implements OfficerClient {

        @Override
        public List<OfficerClientDto> getOfficersByCompanyId(UUID companyId) {
            return null;
        }
    }
}

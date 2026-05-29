package com.company.officerservice.infrastructure.feign;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyClientAdapter implements CompanyValidationPort {

    private static final Logger log = LoggerFactory.getLogger(CompanyClientAdapter.class);

    private final CompanyClient companyClient;
    private final KnownCompanyRepository knownCompanies;

    public CompanyClientAdapter(CompanyClient companyClient, KnownCompanyRepository knownCompanies) {
        this.companyClient = companyClient;
        this.knownCompanies = knownCompanies;
    }

    @Override
    public boolean companyExists(UUID companyId) {
        try {
            companyClient.getCompany(companyId);
            return true;
        } catch (FeignException.NotFound e) {
            return false;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify company {} via company-service: {} — {}",
                    companyId, e.getClass().getName(), e.getMessage(), e);
            throw new ServiceUnavailableException("Cannot verify company — try again later");
        }
    }

    /**
     * Resolves the company owner from the locally maintained {@code known_companies}
     * projection (fed by company-events), not via a synchronous call to company-service.
     * A synchronous call here would create an officer -> company -> officer request cycle,
     * since company-service embeds officers by calling back into officer-service.
     */
    @Override
    public Optional<UUID> findOwnerId(UUID companyId) {
        return knownCompanies.findById(companyId).map(c -> c.getOwnerId());
    }
}

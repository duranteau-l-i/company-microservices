package com.company.officerservice.infrastructure.feign;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CompanyClientAdapter implements CompanyValidationPort {

    private final CompanyClient companyClient;

    public CompanyClientAdapter(CompanyClient companyClient) {
        this.companyClient = companyClient;
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
            throw new ServiceUnavailableException("Cannot verify company — try again later");
        }
    }
}

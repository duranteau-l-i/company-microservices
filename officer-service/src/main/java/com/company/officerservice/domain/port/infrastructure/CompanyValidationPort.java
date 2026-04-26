package com.company.officerservice.domain.port.infrastructure;

import java.util.UUID;

public interface CompanyValidationPort {
    boolean companyExists(UUID companyId);
}

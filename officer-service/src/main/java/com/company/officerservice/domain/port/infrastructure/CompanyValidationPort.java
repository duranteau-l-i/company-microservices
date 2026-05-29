package com.company.officerservice.domain.port.infrastructure;

import java.util.Optional;
import java.util.UUID;

public interface CompanyValidationPort {
    boolean companyExists(UUID companyId);
    Optional<UUID> findOwnerId(UUID companyId);
}

package com.company.officerservice.domain.port.infrastructure;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfficerQueryRepository {
    Optional<OfficerFullView> findFullById(OfficerId id);
    Optional<OfficerRestrictedView> findRestrictedById(OfficerId id);
    List<OfficerFullView> findByCompanyId(UUID companyId);
    List<OfficerRestrictedView> search(String firstName, String lastName, LocalDate dateOfBirth);
    OfficerFullView save(OfficerFullView view);
    void deleteById(OfficerId id);
}

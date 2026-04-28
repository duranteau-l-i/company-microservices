package com.company.companyservice.domain.port.infrastructure;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyQueryRepository {
    Optional<CompanyFullView> findFullById(CompanyId id);
    Optional<CompanyRestrictedView> findRestrictedById(CompanyId id);
    List<CompanyFullView> findAllFull();
    List<CompanyFullView> findFullByOwnerId(UUID ownerId);
    List<CompanyFullView> findAllByOfficerId(UUID officerId);
    List<CompanyRestrictedView> search(String query);
    CompanyFullView save(CompanyFullView view);
    void deleteById(CompanyId id);
}

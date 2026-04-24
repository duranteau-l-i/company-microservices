package com.company.companyservice.domain.port.usecases;

import com.company.companyservice.domain.model.CompanyRestrictedView;

import java.util.List;

public interface SearchCompaniesUseCase {
    List<CompanyRestrictedView> search(Query query);

    record Query(String term) {}
}

package com.company.companyservice.application.query;

import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.SearchCompaniesUseCase;

import java.util.List;

public class SearchCompaniesHandler implements SearchCompaniesUseCase {

    private final CompanyQueryRepository queryRepo;

    public SearchCompaniesHandler(CompanyQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public List<CompanyRestrictedView> search(Query query) {
        return queryRepo.search(query.term());
    }
}

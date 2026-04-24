package com.company.companyservice.config;

import com.company.companyservice.application.command.CreateCompanyHandler;
import com.company.companyservice.application.command.DeleteCompanyHandler;
import com.company.companyservice.application.command.UpdateCompanyHandler;
import com.company.companyservice.application.query.GetCompanyHandler;
import com.company.companyservice.application.query.ListCompaniesHandler;
import com.company.companyservice.application.query.SearchCompaniesHandler;
import com.company.companyservice.domain.port.infrastructure.CompanyCommandRepository;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.CreateCompanyUseCase;
import com.company.companyservice.domain.port.usecases.DeleteCompanyUseCase;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;
import com.company.companyservice.domain.port.usecases.ListCompaniesUseCase;
import com.company.companyservice.domain.port.usecases.SearchCompaniesUseCase;
import com.company.companyservice.domain.port.usecases.UpdateCompanyUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateCompanyUseCase createCompanyUseCase(
            CompanyCommandRepository commandRepository,
            CompanyQueryRepository queryRepository,
            CompanyEventPublisher publisher) {
        return new CreateCompanyHandler(commandRepository, queryRepository, publisher);
    }

    @Bean
    public UpdateCompanyUseCase updateCompanyUseCase(
            CompanyCommandRepository commandRepository,
            CompanyQueryRepository queryRepository,
            CompanyEventPublisher publisher) {
        return new UpdateCompanyHandler(commandRepository, queryRepository, publisher);
    }

    @Bean
    public DeleteCompanyUseCase deleteCompanyUseCase(
            CompanyCommandRepository commandRepository,
            CompanyQueryRepository queryRepository,
            CompanyEventPublisher publisher) {
        return new DeleteCompanyHandler(commandRepository, queryRepository, publisher);
    }

    @Bean
    public GetCompanyUseCase getCompanyUseCase(CompanyQueryRepository queryRepository) {
        return new GetCompanyHandler(queryRepository);
    }

    @Bean
    public ListCompaniesUseCase listCompaniesUseCase(CompanyQueryRepository queryRepository) {
        return new ListCompaniesHandler(queryRepository);
    }

    @Bean
    public SearchCompaniesUseCase searchCompaniesUseCase(CompanyQueryRepository queryRepository) {
        return new SearchCompaniesHandler(queryRepository);
    }
}

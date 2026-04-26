package com.company.officerservice.config;

import com.company.officerservice.application.command.CreateOfficerHandler;
import com.company.officerservice.application.command.DeleteOfficerHandler;
import com.company.officerservice.application.command.LinkOfficerToCompanyHandler;
import com.company.officerservice.application.command.UnlinkOfficerFromCompanyHandler;
import com.company.officerservice.application.command.UpdateOfficerHandler;
import com.company.officerservice.application.query.GetOfficerHandler;
import com.company.officerservice.application.query.ListCompaniesByOfficerHandler;
import com.company.officerservice.application.query.ListOfficersByCompanyHandler;
import com.company.officerservice.application.query.SearchOfficersHandler;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.CreateOfficerUseCase;
import com.company.officerservice.domain.port.usecases.DeleteOfficerUseCase;
import com.company.officerservice.domain.port.usecases.GetOfficerUseCase;
import com.company.officerservice.domain.port.usecases.LinkOfficerToCompanyUseCase;
import com.company.officerservice.domain.port.usecases.ListCompaniesByOfficerUseCase;
import com.company.officerservice.domain.port.usecases.ListOfficersByCompanyUseCase;
import com.company.officerservice.domain.port.usecases.SearchOfficersUseCase;
import com.company.officerservice.domain.port.usecases.UnlinkOfficerFromCompanyUseCase;
import com.company.officerservice.domain.port.usecases.UpdateOfficerUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateOfficerUseCase createOfficerUseCase(
            OfficerCommandRepository commandRepo, OfficerQueryRepository queryRepo,
            OfficerEventPublisher publisher) {
        return new CreateOfficerHandler(commandRepo, queryRepo, publisher);
    }

    @Bean
    public UpdateOfficerUseCase updateOfficerUseCase(
            OfficerCommandRepository commandRepo, OfficerQueryRepository queryRepo,
            OfficerEventPublisher publisher) {
        return new UpdateOfficerHandler(commandRepo, queryRepo, publisher);
    }

    @Bean
    public DeleteOfficerUseCase deleteOfficerUseCase(
            OfficerCommandRepository commandRepo, OfficerQueryRepository queryRepo,
            OfficerEventPublisher publisher) {
        return new DeleteOfficerHandler(commandRepo, queryRepo, publisher);
    }

    @Bean
    public LinkOfficerToCompanyUseCase linkOfficerToCompanyUseCase(
            OfficerCommandRepository commandRepo, OfficerQueryRepository queryRepo,
            OfficerEventPublisher publisher, CompanyValidationPort companyValidationPort) {
        return new LinkOfficerToCompanyHandler(commandRepo, queryRepo, publisher, companyValidationPort);
    }

    @Bean
    public UnlinkOfficerFromCompanyUseCase unlinkOfficerFromCompanyUseCase(
            OfficerCommandRepository commandRepo, OfficerQueryRepository queryRepo,
            OfficerEventPublisher publisher) {
        return new UnlinkOfficerFromCompanyHandler(commandRepo, queryRepo, publisher);
    }

    @Bean
    public GetOfficerUseCase getOfficerUseCase(OfficerQueryRepository queryRepo) {
        return new GetOfficerHandler(queryRepo);
    }

    @Bean
    public SearchOfficersUseCase searchOfficersUseCase(OfficerQueryRepository queryRepo) {
        return new SearchOfficersHandler(queryRepo);
    }

    @Bean
    public ListOfficersByCompanyUseCase listOfficersByCompanyUseCase(OfficerQueryRepository queryRepo) {
        return new ListOfficersByCompanyHandler(queryRepo);
    }

    @Bean
    public ListCompaniesByOfficerUseCase listCompaniesByOfficerUseCase(OfficerQueryRepository queryRepo) {
        return new ListCompaniesByOfficerHandler(queryRepo);
    }
}

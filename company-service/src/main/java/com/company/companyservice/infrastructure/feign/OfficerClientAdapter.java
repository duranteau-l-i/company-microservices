package com.company.companyservice.infrastructure.feign;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OfficerClientAdapter implements OfficerQueryPort {

    private static final Logger log = LoggerFactory.getLogger(OfficerClientAdapter.class);

    private final OfficerClient officerClient;

    public OfficerClientAdapter(OfficerClient officerClient) {
        this.officerClient = officerClient;
    }

    @Override
    public OfficerQueryResult findOfficersByCompanyId(CompanyId companyId) {
        List<OfficerClientDto> dtos;
        try {
            dtos = officerClient.getOfficersByCompanyId(companyId.value());
        } catch (Exception e) {
            log.warn("Failed to fetch officers from officer-service for companyId={}: {}",
                    companyId.value(), e.getMessage());
            return new OfficerQueryResult(List.of(), true);
        }

        // null signals that the fallback factory returned (circuit open / call failed)
        if (dtos == null) {
            return new OfficerQueryResult(List.of(), true);
        }

        List<OfficerSummary> officers = dtos.stream()
                .map(dto -> toOfficerSummary(dto, companyId))
                .filter(Objects::nonNull)
                .toList();

        return new OfficerQueryResult(officers, false);
    }

    private OfficerSummary toOfficerSummary(OfficerClientDto dto, CompanyId companyId) {
        if (dto.companyLinks() == null) {
            return null;
        }
        String title = dto.companyLinks().stream()
                .filter(link -> companyId.value().equals(link.companyId()))
                .map(OfficerClientDto.CompanyLinkDto::title)
                .findFirst()
                .orElse(null);

        if (title == null) {
            return null;
        }

        return new OfficerSummary(dto.id(), dto.firstName(), dto.lastName(), title);
    }
}

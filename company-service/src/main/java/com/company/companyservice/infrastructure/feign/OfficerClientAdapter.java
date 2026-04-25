package com.company.companyservice.infrastructure.feign;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class OfficerClientAdapter implements OfficerQueryPort {

    private final OfficerClient officerClient;

    public OfficerClientAdapter(OfficerClient officerClient) {
        this.officerClient = officerClient;
    }

    @Override
    public OfficerQueryResult findOfficersByCompanyId(CompanyId companyId) {
        OfficerClientFallbackFactory.FALLBACK_FIRED.remove();
        List<OfficerClientDto> dtos = officerClient.getOfficersByCompanyId(companyId.value());
        boolean fallback = Boolean.TRUE.equals(OfficerClientFallbackFactory.FALLBACK_FIRED.get());
        OfficerClientFallbackFactory.FALLBACK_FIRED.remove();

        if (fallback) {
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

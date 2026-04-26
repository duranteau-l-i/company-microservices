package com.company.companyservice.domain.port.infrastructure;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.OfficerSummary;

import java.util.List;

public interface OfficerQueryPort {

    record OfficerQueryResult(List<OfficerSummary> officers, boolean fallback) {}

    OfficerQueryResult findOfficersByCompanyId(CompanyId companyId);
}

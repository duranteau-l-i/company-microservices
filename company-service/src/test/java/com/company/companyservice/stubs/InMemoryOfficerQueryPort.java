package com.company.companyservice.stubs;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryOfficerQueryPort implements OfficerQueryPort {

    private final Map<CompanyId, List<OfficerSummary>> store = new HashMap<>();
    private boolean simulateFallback = false;

    public void addOfficer(CompanyId companyId, OfficerSummary officer) {
        store.computeIfAbsent(companyId, k -> new ArrayList<>()).add(officer);
    }

    public void setSimulateFallback(boolean simulateFallback) {
        this.simulateFallback = simulateFallback;
    }

    public void clear() {
        store.clear();
        simulateFallback = false;
    }

    @Override
    public OfficerQueryResult findOfficersByCompanyId(CompanyId companyId) {
        if (simulateFallback) {
            return new OfficerQueryResult(List.of(), true);
        }
        List<OfficerSummary> officers = store.getOrDefault(companyId, List.of());
        return new OfficerQueryResult(List.copyOf(officers), false);
    }
}

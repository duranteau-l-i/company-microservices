package com.company.companyservice.unit.application.stubs;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCompanyQueryRepository implements CompanyQueryRepository {

    private final Map<CompanyId, CompanyFullView> store = new HashMap<>();

    @Override
    public Optional<CompanyFullView> findFullById(CompanyId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<CompanyRestrictedView> findRestrictedById(CompanyId id) {
        return Optional.ofNullable(store.get(id)).map(this::toRestricted);
    }

    @Override
    public List<CompanyFullView> findAllFull() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<CompanyFullView> findFullByOwnerId(UUID ownerId) {
        return store.values().stream()
                .filter(v -> v.ownerId().equals(ownerId))
                .toList();
    }

    @Override
    public List<CompanyRestrictedView> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return store.values().stream()
                .filter(v -> v.name().toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toRestricted)
                .toList();
    }

    @Override
    public CompanyFullView save(CompanyFullView view) {
        store.put(view.id(), view);
        return view;
    }

    @Override
    public void deleteById(CompanyId id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    public boolean contains(CompanyId id) {
        return store.containsKey(id);
    }

    private CompanyRestrictedView toRestricted(CompanyFullView full) {
        return new CompanyRestrictedView(
                full.id(),
                full.name(),
                full.registrationNumber(),
                full.ownerId(),
                full.ownerDisplayName(),
                full.status()
        );
    }
}

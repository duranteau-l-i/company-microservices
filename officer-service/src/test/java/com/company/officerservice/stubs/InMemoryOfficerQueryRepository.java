package com.company.officerservice.stubs;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryOfficerQueryRepository implements OfficerQueryRepository {

    private final Map<OfficerId, OfficerFullView> store = new HashMap<>();

    @Override
    public Optional<OfficerFullView> findFullById(OfficerId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<OfficerRestrictedView> findRestrictedById(OfficerId id) {
        return Optional.ofNullable(store.get(id)).map(this::toRestricted);
    }

    @Override
    public List<OfficerFullView> findByCompanyId(UUID companyId) {
        return store.values().stream()
                .filter(v -> v.companyLinks().stream()
                        .anyMatch(l -> l.companyId().equals(companyId) && l.active()))
                .toList();
    }

    @Override
    public List<OfficerRestrictedView> search(String firstName, String lastName, LocalDate dateOfBirth) {
        return store.values().stream()
                .filter(v -> matchesSearch(v, firstName, lastName, dateOfBirth))
                .map(this::toRestricted)
                .toList();
    }

    @Override
    public OfficerFullView save(OfficerFullView view) {
        store.put(view.id(), view);
        return view;
    }

    @Override
    public void deleteById(OfficerId id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }

    public boolean contains(OfficerId id) {
        return store.containsKey(id);
    }

    private boolean matchesSearch(OfficerFullView v, String firstName, String lastName, LocalDate dateOfBirth) {
        if (firstName != null && !firstName.isBlank()
                && !v.firstName().toLowerCase(Locale.ROOT).contains(firstName.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (lastName != null && !lastName.isBlank()
                && !v.lastName().toLowerCase(Locale.ROOT).contains(lastName.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (dateOfBirth != null && !v.dateOfBirth().equals(dateOfBirth)) {
            return false;
        }
        return true;
    }

    private OfficerRestrictedView toRestricted(OfficerFullView full) {
        return new OfficerRestrictedView(
                full.id(),
                full.firstName(),
                full.lastName(),
                full.companyLinks()
        );
    }
}

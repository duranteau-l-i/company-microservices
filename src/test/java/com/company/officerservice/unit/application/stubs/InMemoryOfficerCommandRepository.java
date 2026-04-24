package com.company.officerservice.unit.application.stubs;

import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryOfficerCommandRepository implements OfficerCommandRepository {

    private final Map<OfficerId, Officer> store = new HashMap<>();

    @Override
    public Officer save(Officer officer) {
        store.put(officer.id(), officer);
        return officer;
    }

    @Override
    public Optional<Officer> findById(OfficerId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Officer> findByNameAndDateOfBirth(String firstName, String lastName, LocalDate dateOfBirth) {
        return store.values().stream()
                .filter(o -> o.firstName().equalsIgnoreCase(firstName)
                        && o.lastName().equalsIgnoreCase(lastName)
                        && o.dateOfBirth().equals(dateOfBirth))
                .toList();
    }

    @Override
    public void delete(OfficerId id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}

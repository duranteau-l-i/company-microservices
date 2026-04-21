package com.company.userservice.unit.application.stubs;

import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.infrastructure.UserQueryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserQueryRepository implements UserQueryRepository {

    private final Map<UserId, UserReadModel> store = new HashMap<>();

    @Override
    public Optional<UserReadModel> findById(UserId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<UserReadModel> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<UserReadModel> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        String needle = query.toLowerCase(Locale.ROOT);

        return store.values().stream()
                .filter(u -> u.email().value().contains(needle)
                        || u.firstName().toLowerCase(Locale.ROOT).contains(needle)
                        || u.lastName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    @Override
    public UserReadModel save(UserReadModel readModel) {
        store.put(readModel.id(), readModel);

        return readModel;
    }

    @Override
    public void deleteById(UserId id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }
}

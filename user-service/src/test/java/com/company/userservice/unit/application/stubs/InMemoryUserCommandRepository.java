package com.company.userservice.unit.application.stubs;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.UserCommandRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserCommandRepository implements UserCommandRepository {

    private final Map<UserId, User> store = new HashMap<>();

    @Override
    public User save(User user) {
        store.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return store.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public void delete(UserId id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}

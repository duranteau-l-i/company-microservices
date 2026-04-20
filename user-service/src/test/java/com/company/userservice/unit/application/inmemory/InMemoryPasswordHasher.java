package com.company.userservice.unit.application.inmemory;

import com.company.userservice.domain.port.out.PasswordHasher;

public class InMemoryPasswordHasher implements PasswordHasher {

    private static final String PREFIX = "hashed::";

    @Override
    public String hash(String rawPassword) {
        return PREFIX + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return hashedPassword.equals(PREFIX + rawPassword);
    }
}

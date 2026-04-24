package com.company.userservice.presentation.controller;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public record AuthenticatedCaller(UserId id, Role role) {

    public static AuthenticatedCaller current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidCredentialsException("Not authenticated");
        }

        UserId id = UserId.of(UUID.fromString(auth.getName()));

        Role role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> Role.valueOf(a.substring(5)))
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException("No role"));

        return new AuthenticatedCaller(id, role);
    }
}

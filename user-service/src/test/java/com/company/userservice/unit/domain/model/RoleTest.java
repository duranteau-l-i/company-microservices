package com.company.userservice.unit.domain.model;

import com.company.userservice.domain.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void adminCanCreateManagerAndUser() {
        assertThat(Role.ADMIN.canCreate(Role.MANAGER)).isTrue();
        assertThat(Role.ADMIN.canCreate(Role.USER)).isTrue();
        assertThat(Role.ADMIN.canCreate(Role.ADMIN)).isFalse();
    }

    @Test
    void managerCanCreateUserOnly() {
        assertThat(Role.MANAGER.canCreate(Role.USER)).isTrue();
        assertThat(Role.MANAGER.canCreate(Role.MANAGER)).isFalse();
        assertThat(Role.MANAGER.canCreate(Role.ADMIN)).isFalse();
    }

    @Test
    void userCannotCreateAnyone() {
        assertThat(Role.USER.canCreate(Role.USER)).isFalse();
        assertThat(Role.USER.canCreate(Role.MANAGER)).isFalse();
        assertThat(Role.USER.canCreate(Role.ADMIN)).isFalse();
    }
}

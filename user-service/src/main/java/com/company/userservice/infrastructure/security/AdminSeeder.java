package com.company.userservice.infrastructure.security;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.port.out.PasswordHasher;
import com.company.userservice.domain.port.out.UserCommandRepository;
import com.company.userservice.domain.port.out.UserEventPublisher;
import com.company.userservice.domain.port.out.UserQueryRepository;
import com.company.userservice.domain.model.UserReadModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    @Bean
    public ApplicationRunner seedDefaultAdmin(
            UserCommandRepository commandRepository,
            UserQueryRepository queryRepository,
            PasswordHasher passwordHasher,
            UserEventPublisher eventPublisher,
            @Value("${app.admin.email:admin@company.com}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("ADMIN_PASSWORD not configured; skipping default admin seeding");
                return;
            }
            boolean adminExists = queryRepository.findAll().stream()
                    .map(UserReadModel::role)
                    .anyMatch(r -> r == Role.ADMIN);
            if (adminExists) {
                return;
            }
            if (commandRepository.existsByEmail(EmailAddress.of(adminEmail))) {
                return;
            }
            User.Created created = User.create(
                    EmailAddress.of(adminEmail),
                    passwordHasher.hash(adminPassword),
                    "System",
                    "Admin",
                    Role.ADMIN);
            commandRepository.save(created.user());
            eventPublisher.publish(created.event());
            log.info("Seeded default admin {}", adminEmail);
        };
    }
}

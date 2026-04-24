package com.company.officerservice.integration.controller;

import com.company.officerservice.config.UseCaseConfig;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.presentation.controller.OfficerController;
import com.company.officerservice.presentation.controller.OfficerDtoMapper;
import com.company.officerservice.presentation.controller.RestExceptionHandler;
import com.company.officerservice.security.JwtAuthenticationFilter;
import com.company.officerservice.security.JwtTokenValidator;
import com.company.officerservice.security.SecurityConfig;
import com.company.officerservice.stubs.InMemoryOfficerCommandRepository;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OfficerController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenValidator.class,
        UseCaseConfig.class,
        OfficerDtoMapper.class,
        RestExceptionHandler.class,
        RestTestConfig.class
})
@TestPropertySource(properties = {
        "app.security.jwt.secret=" + TestJwtHelper.TEST_SECRET,
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class OfficerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryOfficerCommandRepository commandRepo;

    @Autowired
    private InMemoryOfficerQueryRepository queryRepo;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void clearStubs() {
        commandRepo.clear();
        queryRepo.clear();
    }

    private Map<String, Object> createRequest() {
        return Map.ofEntries(
                Map.entry("companyId", companyId.toString()),
                Map.entry("companyOwnerId", ownerId.toString()),
                Map.entry("firstName", "Alice"),
                Map.entry("lastName", "Smith"),
                Map.entry("dateOfBirth", "1990-01-15"),
                Map.entry("nationality", "French"),
                Map.entry("street", "1 Rue de la Paix"),
                Map.entry("city", "Paris"),
                Map.entry("postalCode", "75001"),
                Map.entry("country", "France"),
                Map.entry("email", "alice@example.com"),
                Map.entry("title", "Director"),
                Map.entry("appointmentDate", "2024-01-01")
        );
    }

    private OfficerFullView seedOfficer(UUID seededOwnerId) {
        Officer.Created created = Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null
        );
        Officer officer = created.officer();
        commandRepo.save(officer);
        OfficerFullView view = new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
        queryRepo.save(view);
        return view;
    }

    // -- POST /api/officers --

    @Test
    void create_asCompanyOwner_returns201() throws Exception {
        String token = TestJwtHelper.accessToken(ownerId, "alice@test.com", Role.USER);

        mockMvc.perform(post("/api/officers")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.companyLinks[0].companyId").value(companyId.toString()));
    }

    @Test
    void create_asManager_returns201() throws Exception {
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "mgr@test.com", Role.MANAGER);

        mockMvc.perform(post("/api/officers")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_nonOwnerUser_returns403() throws Exception {
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "other@test.com", Role.USER);

        mockMvc.perform(post("/api/officers")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/officers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    // -- GET /api/officers/{id} --

    @Test
    void get_asManager_returnsFullView() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "mgr@test.com", Role.MANAGER);

        mockMvc.perform(get("/api/officers/" + view.id().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.nationality").value("French"));
    }

    @Test
    void get_asUser_returnsRestrictedView() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "user@test.com", Role.USER);

        mockMvc.perform(get("/api/officers/" + view.id().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "user@test.com", Role.USER);

        mockMvc.perform(get("/api/officers/" + UUID.randomUUID())
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    // -- PUT /api/officers/{id} --

    @Test
    void update_asManager_returns200() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "mgr@test.com", Role.MANAGER);

        Map<String, Object> updateRequest = Map.of(
                "firstName", "Alicia",
                "lastName", "Smith",
                "dateOfBirth", "1990-01-15",
                "nationality", "Spanish",
                "street", "2 Calle",
                "city", "Madrid",
                "postalCode", "28001",
                "country", "Spain",
                "email", "alicia@example.com"
        );

        mockMvc.perform(put("/api/officers/" + view.id().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alicia"));
    }

    @Test
    void update_asUser_returns403() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(ownerId, "alice@test.com", Role.USER);

        Map<String, Object> updateRequest = Map.of(
                "firstName", "Alicia", "lastName", "Smith",
                "dateOfBirth", "1990-01-15", "nationality", "French",
                "street", "1 Rue", "city", "Paris",
                "postalCode", "75001", "country", "France",
                "email", "alice@example.com"
        );

        mockMvc.perform(put("/api/officers/" + view.id().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    // -- DELETE /api/officers/{id} --

    @Test
    void delete_asAdmin_returns204() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "admin@test.com", Role.ADMIN);

        mockMvc.perform(delete("/api/officers/" + view.id().value())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asManager_returns403() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "mgr@test.com", Role.MANAGER);

        mockMvc.perform(delete("/api/officers/" + view.id().value())
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    // -- GET /api/officers/search --

    @Test
    void search_returnsRestrictedViews() throws Exception {
        seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(UUID.randomUUID(), "user@test.com", Role.USER);

        mockMvc.perform(get("/api/officers/search")
                        .param("firstName", "Alice")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    // -- POST /api/officers/{id}/links --

    @Test
    void link_asCompanyOwner_returns200() throws Exception {
        OfficerFullView view = seedOfficer(ownerId);
        String token = TestJwtHelper.accessToken(ownerId, "owner@test.com", Role.USER);
        UUID newCompanyId = UUID.randomUUID();

        Map<String, Object> linkRequest = Map.of(
                "companyId", newCompanyId.toString(),
                "companyOwnerId", ownerId.toString(),
                "title", "CEO",
                "appointmentDate", "2024-06-01"
        );

        mockMvc.perform(post("/api/officers/" + view.id().value() + "/links")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyLinks[0].title").value("CEO"));
    }

    // -- DELETE /api/officers/{id}/links/{companyId} --

    @Test
    void unlink_asCompanyOwner_returns200() throws Exception {
        Officer.Created created = Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null
        );
        Officer officer = created.officer();
        officer.linkToCompany(CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1)));
        commandRepo.save(officer);
        OfficerFullView view = new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
        queryRepo.save(view);

        String token = TestJwtHelper.accessToken(ownerId, "owner@test.com", Role.USER);

        mockMvc.perform(delete("/api/officers/" + officer.id().value() + "/links/" + companyId)
                        .param("companyOwnerId", ownerId.toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyLinks[0].active").value(false));
    }
}

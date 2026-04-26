package com.company.companyservice.integration.controller;

import com.company.companyservice.config.UseCaseConfig;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.presentation.controller.CompanyController;
import com.company.companyservice.presentation.controller.CompanyDtoMapper;
import com.company.companyservice.presentation.controller.RestExceptionHandler;
import com.company.companyservice.security.JwtAuthenticationFilter;
import com.company.companyservice.security.JwtTokenValidator;
import com.company.companyservice.security.SecurityConfig;
import com.company.companyservice.stubs.InMemoryCompanyCommandRepository;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
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
        controllers = CompanyController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenValidator.class,
        UseCaseConfig.class,
        CompanyDtoMapper.class,
        RestExceptionHandler.class,
        RestTestConfig.class
})
@TestPropertySource(properties = {
        "app.security.jwt.secret=" + TestJwtHelper.TEST_SECRET,
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class CompanyControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryCompanyCommandRepository commandRepo;

    @Autowired
    private InMemoryCompanyQueryRepository queryRepo;

    @BeforeEach
    void clearStubs() {
        commandRepo.clear();
        queryRepo.clear();
    }

    // -- Helpers --

    private Map<String, String> createRequest(String name, String regNum) {
        return Map.of(
                "name", name,
                "registrationNumber", regNum,
                "street", "123 Main St",
                "city", "Paris",
                "postalCode", "75001",
                "country", "France",
                "ownerDisplayName", "Alice Dupont"
        );
    }

    private CompanyFullView seedCompany(UUID ownerId, String name, String regNum) {
        CompanyId id = CompanyId.generate();
        CompanyFullView view = new CompanyFullView(
                id, name, regNum,
                new Address("123 Main St", "Paris", "75001", "France"),
                ownerId, "Alice Dupont", CompanyStatus.ACTIVE,
                Instant.now(), Instant.now(), List.of()
        );
        com.company.companyservice.domain.model.Company company = new com.company.companyservice.domain.model.Company(
                id, name, regNum,
                new Address("123 Main St", "Paris", "75001", "France"),
                ownerId, CompanyStatus.ACTIVE,
                Instant.now(), Instant.now()
        );
        commandRepo.save(company);
        queryRepo.save(view);
        return view;
    }

    // -- POST /api/companies --

    @Test
    void create_withValidUserToken_returns201() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.accessToken(userId, "alice@test.com", Role.USER);

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createRequest("Acme Corp", "REG-" + UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createRequest("Acme Corp", "REG-001"))))
                .andExpect(status().isUnauthorized());
    }

    // MANAGER role cannot create companies (spec rule, enforced in CreateCompanyHandler)
    @Test
    void create_withManagerToken_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.accessToken(userId, "mgr@test.com", Role.MANAGER);

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createRequest("Some Corp", "REG-MGR-" + UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withMissingField_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.accessToken(userId, "alice@test.com", Role.USER);

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "",
                                "registrationNumber", "REG-001",
                                "street", "123 Main St",
                                "city", "Paris",
                                "postalCode", "75001",
                                "country", "France",
                                "ownerDisplayName", "Alice"))))
                .andExpect(status().isBadRequest());
    }

    // -- GET /api/companies/{id} --

    @Test
    void get_asOwner_returnsFullResponse() throws Exception {
        UUID ownerId = UUID.randomUUID();
        CompanyFullView company = seedCompany(ownerId, "Owner Corp", "REG-OWN-001");
        String token = TestJwtHelper.accessToken(ownerId, "alice@test.com", Role.USER);

        mockMvc.perform(get("/api/companies/" + company.id().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.id().value().toString()))
                .andExpect(jsonPath("$.name").value("Owner Corp"))
                .andExpect(jsonPath("$.registrationNumber").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void get_asNonOwnerUser_returnsRestrictedResponse() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        CompanyFullView company = seedCompany(ownerId, "Hidden Corp", "REG-HID-001");
        String token = TestJwtHelper.accessToken(otherUserId, "other@test.com", Role.USER);

        mockMvc.perform(get("/api/companies/" + company.id().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.id().value().toString()))
                .andExpect(jsonPath("$.name").value("Hidden Corp"))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void get_nonExistent_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtHelper.accessToken(userId, "alice@test.com", Role.USER);

        mockMvc.perform(get("/api/companies/" + UUID.randomUUID())
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/companies/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // -- PUT /api/companies/{id} --

    @Test
    void update_asOwner_returns200() throws Exception {
        UUID ownerId = UUID.randomUUID();
        CompanyFullView company = seedCompany(ownerId, "Old Name", "REG-UPD-001");
        String token = TestJwtHelper.accessToken(ownerId, "alice@test.com", Role.USER);

        mockMvc.perform(put("/api/companies/" + company.id().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "New Name",
                                "registrationNumber", "REG-UPD-001",
                                "street", "456 New St",
                                "city", "Lyon",
                                "postalCode", "69001",
                                "country", "France"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void update_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/companies/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "X", "registrationNumber", "Y",
                                "street", "Z", "city", "C", "postalCode", "P", "country", "FR"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_byNonOwnerUser_returns403() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID(); // different user — not the owner
        CompanyFullView company = seedCompany(ownerId, "Protected Corp", "REG-NONOWN-UPD");
        String token = TestJwtHelper.accessToken(callerId, "other@test.com", Role.USER);

        mockMvc.perform(put("/api/companies/" + company.id().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Hacked Name", "registrationNumber", "REG-NONOWN-UPD",
                                "street", "1 Main", "city", "Paris",
                                "postalCode", "75001", "country", "France"))))
                .andExpect(status().isForbidden());
    }

    // -- DELETE /api/companies/{id} --

    @Test
    void delete_asOwner_returns204() throws Exception {
        UUID ownerId = UUID.randomUUID();
        CompanyFullView company = seedCompany(ownerId, "Delete Corp", "REG-DEL-001");
        String token = TestJwtHelper.accessToken(ownerId, "alice@test.com", Role.USER);

        mockMvc.perform(delete("/api/companies/" + company.id().value())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/companies/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_byNonOwnerUser_returns403() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID(); // different user — not the owner
        CompanyFullView company = seedCompany(ownerId, "Protected Corp", "REG-NONOWN-DEL");
        String token = TestJwtHelper.accessToken(callerId, "other@test.com", Role.USER);

        mockMvc.perform(delete("/api/companies/" + company.id().value())
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    // -- GET /api/companies --

    @Test
    void list_asUser_returnsOwnCompanies() throws Exception {
        UUID userId = UUID.randomUUID();
        seedCompany(userId, "My Corp", "REG-LIST-001");
        seedCompany(UUID.randomUUID(), "Other Corp", "REG-LIST-002");
        String token = TestJwtHelper.accessToken(userId, "alice@test.com", Role.USER);

        mockMvc.perform(get("/api/companies")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("My Corp"));
    }

    @Test
    void list_asAdmin_returnsAllCompanies() throws Exception {
        UUID adminId = UUID.randomUUID();
        seedCompany(UUID.randomUUID(), "Corp A", "REG-ADM-001");
        seedCompany(UUID.randomUUID(), "Corp B", "REG-ADM-002");
        String token = TestJwtHelper.accessToken(adminId, "admin@test.com", Role.ADMIN);

        mockMvc.perform(get("/api/companies")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isUnauthorized());
    }

    // -- GET /api/companies/search --

    @Test
    void search_returnsMatchingRestrictedViews() throws Exception {
        UUID userId = UUID.randomUUID();
        seedCompany(UUID.randomUUID(), "Acme Industries", "REG-SRH-001");
        seedCompany(UUID.randomUUID(), "Global Corp", "REG-SRH-002");
        String token = TestJwtHelper.accessToken(userId, "alice@test.com", Role.USER);

        mockMvc.perform(get("/api/companies/search")
                        .param("term", "acme")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Acme Industries"))
                .andExpect(jsonPath("$[0].address").doesNotExist());
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/companies/search").param("term", "acme"))
                .andExpect(status().isUnauthorized());
    }
}

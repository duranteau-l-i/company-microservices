package com.company.companyservice.presentation.controller;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.CreateCompanyUseCase;
import com.company.companyservice.domain.port.usecases.DeleteCompanyUseCase;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;
import com.company.companyservice.domain.port.usecases.ListCompaniesUseCase;
import com.company.companyservice.domain.port.usecases.SearchCompaniesUseCase;
import com.company.companyservice.domain.port.usecases.UpdateCompanyUseCase;
import com.company.companyservice.security.CompanyUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Company management endpoints")
public class CompanyController {

    private final CreateCompanyUseCase createCompanyUseCase;
    private final UpdateCompanyUseCase updateCompanyUseCase;
    private final DeleteCompanyUseCase deleteCompanyUseCase;
    private final GetCompanyUseCase getCompanyUseCase;
    private final ListCompaniesUseCase listCompaniesUseCase;
    private final SearchCompaniesUseCase searchCompaniesUseCase;
    private final CompanyDtoMapper mapper;

    public CompanyController(
            CreateCompanyUseCase createCompanyUseCase,
            UpdateCompanyUseCase updateCompanyUseCase,
            DeleteCompanyUseCase deleteCompanyUseCase,
            GetCompanyUseCase getCompanyUseCase,
            ListCompaniesUseCase listCompaniesUseCase,
            SearchCompaniesUseCase searchCompaniesUseCase,
            CompanyDtoMapper mapper) {
        this.createCompanyUseCase = createCompanyUseCase;
        this.updateCompanyUseCase = updateCompanyUseCase;
        this.deleteCompanyUseCase = deleteCompanyUseCase;
        this.getCompanyUseCase = getCompanyUseCase;
        this.listCompaniesUseCase = listCompaniesUseCase;
        this.searchCompaniesUseCase = searchCompaniesUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create a new company")
    public ResponseEntity<CompanyFullResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyUserDetails user = currentUser();
        CreateCompanyUseCase.Command command = new CreateCompanyUseCase.Command(
                UUID.fromString(user.userId()),
                currentRole(),
                request.ownerDisplayName(),
                request.name(),
                request.registrationNumber(),
                request.street(),
                request.city(),
                request.postalCode(),
                request.country()
        );
        CompanyFullView view = createCompanyUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toFullResponse(view));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a company by ID (full view for owner/admin, restricted for others)")
    public ResponseEntity<Object> get(@PathVariable UUID id) {
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(
                UUID.fromString(currentUser().userId()),
                currentRole(),
                CompanyId.of(id)
        );
        GetCompanyUseCase.Result result = getCompanyUseCase.get(query);
        Object response = switch (result.view()) {
            case CompanyFullView full -> mapper.toFullResponse(full, result.warnings());
            case CompanyRestrictedView restricted -> mapper.toRestrictedResponse(restricted);
        };
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a company (owner or ADMIN only)")
    public ResponseEntity<CompanyFullResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                UUID.fromString(currentUser().userId()),
                currentRole(),
                CompanyId.of(id),
                request.name(),
                request.registrationNumber(),
                request.street(),
                request.city(),
                request.postalCode(),
                request.country()
        );
        CompanyFullView view = updateCompanyUseCase.update(command);
        return ResponseEntity.ok(mapper.toFullResponse(view));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a company (owner or ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        DeleteCompanyUseCase.Command command = new DeleteCompanyUseCase.Command(
                UUID.fromString(currentUser().userId()),
                currentRole(),
                CompanyId.of(id)
        );
        deleteCompanyUseCase.delete(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List companies (USER sees own, MANAGER/ADMIN see all)")
    public ResponseEntity<List<CompanyFullResponse>> list() {
        ListCompaniesUseCase.Query query = new ListCompaniesUseCase.Query(
                UUID.fromString(currentUser().userId()),
                currentRole()
        );
        List<CompanyFullView> views = listCompaniesUseCase.list(query);
        return ResponseEntity.ok(views.stream().map(mapper::toFullResponse).toList());
    }

    @GetMapping("/search")
    @Operation(summary = "Search companies by name or registration number (restricted view)")
    public ResponseEntity<List<CompanyRestrictedResponse>> search(@RequestParam String term) {
        SearchCompaniesUseCase.Query query = new SearchCompaniesUseCase.Query(term);
        List<CompanyRestrictedView> views = searchCompaniesUseCase.search(query);
        return ResponseEntity.ok(views.stream().map(mapper::toRestrictedResponse).toList());
    }

    private CompanyUserDetails currentUser() {
        return (CompanyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Role currentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(a -> {
                    try {
                        return Role.valueOf(a.getAuthority().replace("ROLE_", ""));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Unknown role in token: " + a.getAuthority());
                    }
                })
                .orElseThrow(() -> new IllegalStateException("No role authority in security context"));
    }
}

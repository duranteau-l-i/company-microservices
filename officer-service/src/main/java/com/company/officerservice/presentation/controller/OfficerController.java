package com.company.officerservice.presentation.controller;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.OfficerView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.CreateOfficerUseCase;
import com.company.officerservice.domain.port.usecases.DeleteOfficerUseCase;
import com.company.officerservice.domain.port.usecases.GetOfficerUseCase;
import com.company.officerservice.domain.port.usecases.LinkOfficerToCompanyUseCase;
import com.company.officerservice.domain.port.usecases.ListCompaniesByOfficerUseCase;
import com.company.officerservice.domain.port.usecases.ListOfficersByCompanyUseCase;
import com.company.officerservice.domain.port.usecases.SearchOfficersUseCase;
import com.company.officerservice.domain.port.usecases.UnlinkOfficerFromCompanyUseCase;
import com.company.officerservice.domain.port.usecases.UpdateOfficerUseCase;
import com.company.officerservice.security.OfficerUserDetails;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/officers")
@Tag(name = "Officers", description = "Officer management endpoints")
public class OfficerController {

    private final CreateOfficerUseCase createOfficerUseCase;
    private final UpdateOfficerUseCase updateOfficerUseCase;
    private final DeleteOfficerUseCase deleteOfficerUseCase;
    private final GetOfficerUseCase getOfficerUseCase;
    private final SearchOfficersUseCase searchOfficersUseCase;
    private final ListOfficersByCompanyUseCase listOfficersByCompanyUseCase;
    private final ListCompaniesByOfficerUseCase listCompaniesByOfficerUseCase;
    private final LinkOfficerToCompanyUseCase linkOfficerToCompanyUseCase;
    private final UnlinkOfficerFromCompanyUseCase unlinkOfficerFromCompanyUseCase;
    private final OfficerDtoMapper mapper;

    public OfficerController(
            CreateOfficerUseCase createOfficerUseCase,
            UpdateOfficerUseCase updateOfficerUseCase,
            DeleteOfficerUseCase deleteOfficerUseCase,
            GetOfficerUseCase getOfficerUseCase,
            SearchOfficersUseCase searchOfficersUseCase,
            ListOfficersByCompanyUseCase listOfficersByCompanyUseCase,
            ListCompaniesByOfficerUseCase listCompaniesByOfficerUseCase,
            LinkOfficerToCompanyUseCase linkOfficerToCompanyUseCase,
            UnlinkOfficerFromCompanyUseCase unlinkOfficerFromCompanyUseCase,
            OfficerDtoMapper mapper) {
        this.createOfficerUseCase = createOfficerUseCase;
        this.updateOfficerUseCase = updateOfficerUseCase;
        this.deleteOfficerUseCase = deleteOfficerUseCase;
        this.getOfficerUseCase = getOfficerUseCase;
        this.searchOfficersUseCase = searchOfficersUseCase;
        this.listOfficersByCompanyUseCase = listOfficersByCompanyUseCase;
        this.listCompaniesByOfficerUseCase = listCompaniesByOfficerUseCase;
        this.linkOfficerToCompanyUseCase = linkOfficerToCompanyUseCase;
        this.unlinkOfficerFromCompanyUseCase = unlinkOfficerFromCompanyUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create a new officer (company owner or MANAGER/ADMIN)")
    public ResponseEntity<OfficerFullResponse> create(@Valid @RequestBody CreateOfficerRequest request) {
        OfficerFullView view = createOfficerUseCase.create(new CreateOfficerUseCase.Command(
                currentUserId(), currentRole(),
                request.companyId(),
                request.firstName(), request.lastName(), request.dateOfBirth(),
                request.nationality(), request.street(), request.city(),
                request.postalCode(), request.country(),
                request.email(), request.phone(),
                request.title(), request.appointmentDate()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toFullResponse(view));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an officer by ID (full view for MANAGER/ADMIN, restricted for USER)")
    public ResponseEntity<Object> get(@PathVariable UUID id) {
        OfficerView view = getOfficerUseCase.get(new GetOfficerUseCase.Command(
                currentUserId(), currentRole(), OfficerId.of(id)
        ));
        Object response = switch (view) {
            case OfficerFullView full -> mapper.toFullResponse(full);
            case OfficerRestrictedView restricted -> mapper.toRestrictedResponse(restricted);
            default -> throw new IllegalStateException("Unknown view type: " + view.getClass());
        };
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an officer (MANAGER or ADMIN only)")
    public ResponseEntity<OfficerFullResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOfficerRequest request) {
        OfficerFullView view = updateOfficerUseCase.update(new UpdateOfficerUseCase.Command(
                currentUserId(), currentRole(), OfficerId.of(id),
                request.firstName(), request.lastName(), request.dateOfBirth(),
                request.nationality(), request.street(), request.city(),
                request.postalCode(), request.country(),
                request.email(), request.phone()
        ));
        return ResponseEntity.ok(mapper.toFullResponse(view));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an officer (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteOfficerUseCase.delete(new DeleteOfficerUseCase.Command(
                currentUserId(), currentRole(), OfficerId.of(id)
        ));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search officers by name and/or date of birth (for deduplication)")
    public ResponseEntity<List<OfficerRestrictedResponse>> search(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) LocalDate dateOfBirth) {
        List<OfficerRestrictedView> views = searchOfficersUseCase.search(
                new SearchOfficersUseCase.Command(currentUserId(), currentRole(), firstName, lastName, dateOfBirth)
        );
        return ResponseEntity.ok(views.stream().map(mapper::toRestrictedResponse).toList());
    }

    @GetMapping("/by-company/{companyId}")
    @Operation(summary = "List all officers linked to a company (full view for MANAGER/ADMIN, restricted for USER)")
    public ResponseEntity<List<Object>> listByCompany(@PathVariable UUID companyId) {
        List<OfficerView> views = listOfficersByCompanyUseCase.list(
                new ListOfficersByCompanyUseCase.Command(currentUserId(), currentRole(), companyId)
        );
        List<Object> responses = views.stream().map(v -> switch (v) {
            case OfficerFullView full -> (Object) mapper.toFullResponse(full);
            case OfficerRestrictedView restricted -> (Object) mapper.toRestrictedResponse(restricted);
            default -> throw new IllegalStateException("Unknown view type: " + v.getClass());
        }).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/companies")
    @Operation(summary = "List all companies an officer is linked to (full view for MANAGER/ADMIN, restricted for USER)")
    public ResponseEntity<Object> listCompanies(@PathVariable UUID id) {
        OfficerView view = listCompaniesByOfficerUseCase.list(
                new ListCompaniesByOfficerUseCase.Command(currentUserId(), currentRole(), OfficerId.of(id))
        );
        Object response = switch (view) {
            case OfficerFullView full -> mapper.toFullResponse(full);
            case OfficerRestrictedView restricted -> mapper.toRestrictedResponse(restricted);
            default -> throw new IllegalStateException("Unknown view type: " + view.getClass());
        };
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/links")
    @Operation(summary = "Link an officer to a company (company owner or MANAGER/ADMIN)")
    public ResponseEntity<OfficerFullResponse> link(
            @PathVariable UUID id,
            @Valid @RequestBody LinkOfficerRequest request) {
        OfficerFullView view = linkOfficerToCompanyUseCase.link(new LinkOfficerToCompanyUseCase.Command(
                currentUserId(), currentRole(),
                OfficerId.of(id), request.companyId(),
                request.title(), request.appointmentDate()
        ));
        return ResponseEntity.ok(mapper.toFullResponse(view));
    }

    @DeleteMapping("/{id}/links/{companyId}")
    @Operation(summary = "Unlink an officer from a company (company owner or MANAGER/ADMIN)")
    public ResponseEntity<OfficerFullResponse> unlink(
            @PathVariable UUID id,
            @PathVariable UUID companyId) {
        OfficerFullView view = unlinkOfficerFromCompanyUseCase.unlink(new UnlinkOfficerFromCompanyUseCase.Command(
                currentUserId(), currentRole(), OfficerId.of(id), companyId
        ));
        return ResponseEntity.ok(mapper.toFullResponse(view));
    }

    private UUID currentUserId() {
        return UUID.fromString(currentUserDetails().userId());
    }

    private OfficerUserDetails currentUserDetails() {
        return (OfficerUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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

package com.company.companyservice.unit.presentation.consumer;

import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.companyservice.presentation.consumer.OfficerEventConsumer;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
import com.company.companyservice.stubs.InMemoryProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfficerEventConsumerTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID OFFICER_ID = UUID.randomUUID();

    private InMemoryCompanyQueryRepository queryRepo;
    private InMemoryProcessedEventRepository processedEvents;
    private OfficerEventConsumer consumer;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryCompanyQueryRepository();
        processedEvents = new InMemoryProcessedEventRepository();
        consumer = new OfficerEventConsumer(queryRepo, processedEvents, new ObjectMapper());
        seedCompanyWithOfficer();
    }

    private void seedCompanyWithOfficer() {
        OfficerSummary officer = new OfficerSummary(OFFICER_ID, "John", "Doe", "Director");
        CompanyFullView company = new CompanyFullView(
                CompanyId.of(COMPANY_ID),
                "Acme Corp",
                "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                UUID.randomUUID(),
                "Alice Owner",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                new ArrayList<>(List.of(officer))
        );
        queryRepo.save(company);
    }

    private String buildEnvelope(String eventType, String payloadJson) {
        UUID eventId = UUID.randomUUID();
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "aggregateId": "%s",
                  "aggregateType": "Officer",
                  "timestamp": "2026-04-19T10:30:00Z",
                  "version": 1,
                  "payload": %s
                }
                """.formatted(eventId, eventType, UUID.randomUUID(), payloadJson);
    }

    private String buildEnvelopeWithId(UUID eventId, String eventType, String payloadJson) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "aggregateId": "%s",
                  "aggregateType": "Officer",
                  "timestamp": "2026-04-19T10:30:00Z",
                  "version": 1,
                  "payload": %s
                }
                """.formatted(eventId, eventType, UUID.randomUUID(), payloadJson);
    }

    @Test
    void handleOfficerUpdated_refreshesFirstNameLastName_preservesTitle() {
        String payload = """
                {
                  "aggregateId": "%s",
                  "firstName": "Jonathan",
                  "lastName": "Doeson",
                  "title": "Director"
                }
                """.formatted(OFFICER_ID);

        consumer.onMessage(buildEnvelope("OfficerUpdatedEvent", payload));

        CompanyFullView updated = queryRepo.findFullById(CompanyId.of(COMPANY_ID)).orElseThrow();
        assertThat(updated.officers()).hasSize(1);
        OfficerSummary updatedOfficer = updated.officers().get(0);
        assertThat(updatedOfficer.officerId()).isEqualTo(OFFICER_ID);
        assertThat(updatedOfficer.firstName()).isEqualTo("Jonathan");
        assertThat(updatedOfficer.lastName()).isEqualTo("Doeson");
        assertThat(updatedOfficer.title()).isEqualTo("Director");
    }

    @Test
    void handleOfficerDeleted_removesOfficerFromCompany() {
        String payload = """
                {
                  "aggregateId": "%s"
                }
                """.formatted(OFFICER_ID);

        consumer.onMessage(buildEnvelope("OfficerDeletedEvent", payload));

        CompanyFullView updated = queryRepo.findFullById(CompanyId.of(COMPANY_ID)).orElseThrow();
        assertThat(updated.officers()).isEmpty();
    }

    @Test
    void handleOfficerUpdated_isIdempotent() {
        UUID eventId = UUID.randomUUID();
        String payload = """
                {
                  "aggregateId": "%s",
                  "firstName": "Jonathan",
                  "lastName": "Doeson",
                  "title": "Director"
                }
                """.formatted(OFFICER_ID);
        String message = buildEnvelopeWithId(eventId, "OfficerUpdatedEvent", payload);

        // Process the same event twice
        consumer.onMessage(message);
        consumer.onMessage(message);

        // The event should only be stored once and only one update applied
        assertThat(processedEvents.count()).isEqualTo(1);
        CompanyFullView updated = queryRepo.findFullById(CompanyId.of(COMPANY_ID)).orElseThrow();
        assertThat(updated.officers()).hasSize(1);
        assertThat(updated.officers().get(0).firstName()).isEqualTo("Jonathan");
    }

    @Test
    void handleOfficerLinked_addsOfficerToCompany() {
        UUID newOfficerId = UUID.randomUUID();
        String payload = """
                {
                  "aggregateId": "%s",
                  "companyId": "%s",
                  "firstName": "Jane",
                  "lastName": "Smith",
                  "title": "CFO"
                }
                """.formatted(newOfficerId, COMPANY_ID);

        consumer.onMessage(buildEnvelope("OfficerLinkedToCompanyEvent", payload));

        CompanyFullView updated = queryRepo.findFullById(CompanyId.of(COMPANY_ID)).orElseThrow();
        assertThat(updated.officers()).hasSize(2);
        assertThat(updated.officers())
                .anyMatch(o -> o.officerId().equals(newOfficerId)
                        && o.firstName().equals("Jane")
                        && o.lastName().equals("Smith")
                        && o.title().equals("CFO"));
    }

    @Test
    void handleOfficerUnlinked_removesOfficerFromCompany() {
        String payload = """
                {
                  "aggregateId": "%s",
                  "companyId": "%s"
                }
                """.formatted(OFFICER_ID, COMPANY_ID);

        consumer.onMessage(buildEnvelope("OfficerUnlinkedFromCompanyEvent", payload));

        CompanyFullView updated = queryRepo.findFullById(CompanyId.of(COMPANY_ID)).orElseThrow();
        assertThat(updated.officers()).isEmpty();
    }

    @Test
    void handleOfficerDeleted_acrossMultipleCompanies() {
        // Add second company with the same officer
        UUID secondCompanyId = UUID.randomUUID();
        OfficerSummary officer = new OfficerSummary(OFFICER_ID, "John", "Doe", "Director");
        CompanyFullView secondCompany = new CompanyFullView(
                CompanyId.of(secondCompanyId),
                "Beta Ltd",
                "REG-002",
                new Address("2 Side St", "Lyon", "69001", "France"),
                UUID.randomUUID(),
                "Bob Owner",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                new ArrayList<>(List.of(officer))
        );
        queryRepo.save(secondCompany);

        String payload = """
                {
                  "aggregateId": "%s"
                }
                """.formatted(OFFICER_ID);

        consumer.onMessage(buildEnvelope("OfficerDeletedEvent", payload));

        assertThat(queryRepo.findFullById(CompanyId.of(COMPANY_ID)).orElseThrow().officers()).isEmpty();
        assertThat(queryRepo.findFullById(CompanyId.of(secondCompanyId)).orElseThrow().officers()).isEmpty();
    }
}

package com.company.officerservice.presentation.consumer;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyDocument;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyRepository;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CompanyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CompanyEventConsumer.class);

    private final OfficerCommandRepository commandRepository;
    private final OfficerQueryRepository queryRepository;
    private final ProcessedEventRepository processedEvents;
    private final KnownCompanyRepository knownCompanies;
    private final ObjectMapper objectMapper;

    public CompanyEventConsumer(
            OfficerCommandRepository commandRepository,
            OfficerQueryRepository queryRepository,
            ProcessedEventRepository processedEvents,
            KnownCompanyRepository knownCompanies,
            ObjectMapper objectMapper) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.processedEvents = processedEvents;
        this.knownCompanies = knownCompanies;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.company-events:company-events}",
            groupId = "officer-service-group")
    public void onMessage(String rawMessage) {
        try {
            JsonNode envelope = objectMapper.readTree(rawMessage);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());

            if (processedEvents.existsById(eventId)) {
                log.debug("Skipping already-processed company event {}", eventId);
                return;
            }

            String eventType = envelope.get("eventType").asText();
            UUID aggregateId = UUID.fromString(envelope.get("aggregateId").asText());

            switch (eventType) {
                case "CompanyCreatedEvent" -> handleCompanyCreated(aggregateId);
                case "CompanyDeletedEvent" -> handleCompanyDeleted(aggregateId);
                default -> log.debug("Ignoring company event type: {}", eventType);
            }

            processedEvents.save(new ProcessedEventDocument(eventId, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to process company event", e);
            throw new RuntimeException(e);
        }
    }

    private void handleCompanyCreated(UUID companyId) {
        knownCompanies.save(new KnownCompanyDocument(companyId));
    }

    private void handleCompanyDeleted(UUID companyId) {
        knownCompanies.deleteById(companyId);
        List<OfficerFullView> linkedOfficers = queryRepository.findByCompanyId(companyId);
        for (OfficerFullView officerView : linkedOfficers) {
            commandRepository.findById(officerView.id()).ifPresent(officer -> {
                try {
                    var unlinked = officer.unlinkFromCompany(companyId);
                    commandRepository.save(unlinked.officer());
                    OfficerFullView updated = new OfficerFullView(
                            unlinked.officer().id(),
                            unlinked.officer().firstName(),
                            unlinked.officer().lastName(),
                            unlinked.officer().dateOfBirth(),
                            unlinked.officer().nationality(),
                            unlinked.officer().address(),
                            unlinked.officer().email(),
                            unlinked.officer().phone(),
                            unlinked.officer().companyLinks(),
                            unlinked.officer().createdAt(),
                            unlinked.officer().updatedAt()
                    );
                    queryRepository.save(updated);
                } catch (Exception e) {
                    log.warn("Could not unlink officer {} from deleted company {}: {}",
                            officerView.id(), companyId, e.getMessage());
                }
            });
        }
    }
}

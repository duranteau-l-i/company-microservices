package com.company.officerservice.infrastructure.persistence.query;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoOfficerQueryRepository implements OfficerQueryRepository {

    private final OfficerMongoRepository mongo;
    private final MongoTemplate mongoTemplate;

    public MongoOfficerQueryRepository(OfficerMongoRepository mongo, MongoTemplate mongoTemplate) {
        this.mongo = mongo;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<OfficerFullView> findFullById(OfficerId id) {
        return mongo.findById(id.value()).map(OfficerDocumentMapper::toFullView);
    }

    @Override
    public Optional<OfficerRestrictedView> findRestrictedById(OfficerId id) {
        return mongo.findById(id.value()).map(OfficerDocumentMapper::toRestrictedView);
    }

    @Override
    public List<OfficerFullView> findByCompanyId(UUID companyId) {
        return mongo.findByActiveCompanyId(companyId).stream()
                .map(OfficerDocumentMapper::toFullView)
                .toList();
    }

    @Override
    public List<OfficerRestrictedView> search(String firstName, String lastName, LocalDate dateOfBirth) {
        Criteria criteria = new Criteria();

        if (firstName != null && !firstName.isBlank()) {
            criteria = criteria.and("firstName").regex(firstName, "i");
        }
        if (lastName != null && !lastName.isBlank()) {
            criteria = criteria.and("lastName").regex(lastName, "i");
        }
        if (dateOfBirth != null) {
            criteria = criteria.and("dateOfBirth").is(dateOfBirth);
        }

        Query query = Query.query(criteria);
        return mongoTemplate.find(query, OfficerDocument.class).stream()
                .map(OfficerDocumentMapper::toRestrictedView)
                .toList();
    }

    @Override
    public OfficerFullView save(OfficerFullView view) {
        OfficerDocument saved = mongo.save(OfficerDocumentMapper.toDocument(view));
        return OfficerDocumentMapper.toFullView(saved);
    }

    @Override
    public void deleteById(OfficerId id) {
        mongo.deleteById(id.value());
    }
}

package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OfficerMongoRepository extends MongoRepository<OfficerDocument, UUID> {

    @Query("{ 'companyLinks': { $elemMatch: { 'companyId': ?0, 'active': true } } }")
    List<OfficerDocument> findByActiveCompanyId(UUID companyId);
}

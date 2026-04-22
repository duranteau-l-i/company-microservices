package com.company.userservice.infrastructure.persistence.query;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserMongoRepository extends MongoRepository<UserDocument, UUID> {

    @Query("{ $or: [ " +
           "{ 'email': { $regex: ?0, $options: 'i' } }, " +
           "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'lastName': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<UserDocument> searchByText(String query);
}

package com.company.companyservice.stubs;

import com.company.companyservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class InMemoryProcessedEventRepository implements ProcessedEventRepository {

    private final Map<UUID, ProcessedEventDocument> store = new HashMap<>();

    @Override
    public boolean existsById(UUID id) {
        return store.containsKey(id);
    }

    @Override
    public <S extends ProcessedEventDocument> S save(S entity) {
        store.put(entity.getEventId(), entity);
        return entity;
    }

    @Override
    public Optional<ProcessedEventDocument> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ProcessedEventDocument> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(UUID id) {
        store.remove(id);
    }

    @Override
    public void delete(ProcessedEventDocument entity) {
        store.remove(entity.getEventId());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public <S extends ProcessedEventDocument> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        entities.forEach(e -> result.add(save(e)));
        return result;
    }

    @Override
    public List<ProcessedEventDocument> findAllById(Iterable<UUID> ids) {
        List<ProcessedEventDocument> result = new ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(result::add));
        return result;
    }

    @Override
    public void deleteAll(Iterable<? extends ProcessedEventDocument> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> ids) {
        ids.forEach(this::deleteById);
    }

    // --- Unsupported methods from MongoRepository / PagingAndSortingRepository ---

    @Override
    public List<ProcessedEventDocument> findAll(Sort sort) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public Page<ProcessedEventDocument> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> S insert(S entity) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> List<S> insert(Iterable<S> entities) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> long count(Example<S> example) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("Not supported in stub");
    }

    @Override
    public <S extends ProcessedEventDocument, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("Not supported in stub");
    }
}

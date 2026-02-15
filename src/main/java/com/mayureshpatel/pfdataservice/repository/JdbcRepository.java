package com.mayureshpatel.pfdataservice.repository;

import java.util.List;
import java.util.Optional;

/**
 * Base interface for JDBC-based repositories.
 * Provides common CRUD operations using a Spring JDBC client.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
public interface JdbcRepository<T, ID> {

    /**
     * Inserts a new entity.
     *
     * @param entity The entity to insert
     * @return The inserted entity
     */
    default T insert(T entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates an existing entity.
     *
     * @param entity The entity to update
     * @return The updated entity
     */
    default T update(T entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Finds an entity by its primary key.
     *
     * @param id The primary key of the entity
     * @return The entity if found, otherwise Optional.empty()
     */
    default Optional<T> findById(ID id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Find all entities.
     *
     * @return List of all entities
     */
    default List<T> findAll() {
        throw new UnsupportedOperationException();
    }

    /**
     * Delete an entity
     *
     * @param id The ID of the entity to delete
     */
    default void deleteById(ID id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Delete an entity by ID.
     * Used for tables that support soft-deletion.
     *
     * @param id The ID of the entity to delete
     * @param deletedBy The ID of the user who deleted the entity
     */
    default void deleteById(ID id, Long deletedBy) {
        throw new UnsupportedOperationException();
    }

    /**
     * Count the number of entities
     *
     * @return The number of entities
     */
    default long count() {
        throw new UnsupportedOperationException();
    }
}

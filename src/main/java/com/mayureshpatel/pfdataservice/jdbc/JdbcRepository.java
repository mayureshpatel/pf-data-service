package com.mayureshpatel.pfdataservice.jdbc;

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
     * Saves or updates an entity.
     *
     * @param entity The entity to save or update
     * @return The saved or updated entity
     */
    default T save(T entity) {
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
     * Delete an entity by ID
     *
     * @param id The ID of the entity to delete
     */
    default void deleteById(ID id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Check if an entity exists by ID
     *
     * @param id The ID of the entity to check
     * @return True if the entity exists, false otherwise
     */
    default boolean existsById(ID id) {
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

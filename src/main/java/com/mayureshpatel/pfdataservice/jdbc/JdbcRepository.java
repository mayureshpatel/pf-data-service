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
    T save(T entity);

    /**
     * Finds an entity by its primary key.
     *
     * @param id The primary key of the entity
     * @return The entity if found, otherwise Optional.empty()
     */
    Optional<T> findById(ID id);

    /**
     * Find all entities.
     *
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Delete an entity by ID
     *
     * @param id The ID of the entity to delete
     */
    void deleteById(ID id);

    /**
     * Check if an entity exists by ID
     *
     * @param id The ID of the entity to check
     * @return True if the entity exists, false otherwise
     */
    boolean existsById(ID id);

    /**
     * Count the number of entities
     *
     * @return The number of entities
     */
    long count();
}

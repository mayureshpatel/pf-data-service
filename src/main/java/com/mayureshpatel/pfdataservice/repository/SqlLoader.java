package com.mayureshpatel.pfdataservice.repository;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for loading SQL queries from external files.
 * Queries are cached after first load for performance.
 */
@Component
public class SqlLoader {

    private final ConcurrentHashMap<String, String> queryCache = new ConcurrentHashMap<>();

    /**
     * Load a SQL query from a file in the classpath.
     *
     * @param path the classpath path to the SQL file (e.g., "sql/currency/findById.sql")
     * @return the SQL query as a string
     * @throws RuntimeException if the file cannot be read
     */
    public String load(String path) {
        return queryCache.computeIfAbsent(path, this::loadFromFile);
    }

    private String loadFromFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SQL query from: " + path, e);
        }
    }

    /**
     * Clear the query cache. Useful for testing or if queries need to be reloaded.
     */
    public void clearCache() {
        queryCache.clear();
    }
}

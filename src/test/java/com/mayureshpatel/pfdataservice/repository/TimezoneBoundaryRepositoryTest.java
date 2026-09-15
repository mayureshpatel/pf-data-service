package com.mayureshpatel.pfdataservice.repository;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for repository tests that specifically need to reproduce PF-828/PF-836's bug class:
 * a query that binds a bare {@code LocalDate} (or uses {@code EXTRACT()}) against a
 * {@code timestamptz} column resolves it using the database session's timezone, not UTC. The
 * default test datasource (see {@code src/test/resources/application.yml}) never sets a session
 * timezone, so it implicitly runs under whatever the Testcontainers Postgres image defaults to
 * (UTC) -- which is exactly why the wider test suite never caught this bug class: it doesn't run
 * under the same timezone production does.
 * <p>
 * Production sets {@code spring.datasource.hikari.connection-init-sql: SET
 * timezone='America/New_York'} ({@code application.yml}). This base class reproduces that same
 * setting for the specific tests that need to prove a query is (or isn't) timezone-safe, without
 * changing the timezone for the rest of the suite -- {@code @DynamicPropertySource} only affects
 * the Spring context built for subclasses of this class, a separate cached context from
 * {@link BaseRepositoryTest}'s own default (UTC) one.
 */
public abstract class TimezoneBoundaryRepositoryTest extends BaseRepositoryTest {

    @DynamicPropertySource
    static void useNonUtcSessionTimezone(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "SET timezone='America/New_York'");
    }
}

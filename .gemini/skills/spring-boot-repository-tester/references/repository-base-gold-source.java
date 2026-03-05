package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.config.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

/**
 * Base class for all Repository layer tests.
 * Uses @JdbcTest for minimal context (only JDBC related beans).
 * Disables the default H2 embedded database replacement to use Testcontainers PostgreSQL.
 * Automatically seeds the database before each test to ensure a clean, consistent baseline.
 *
 * We use @ComponentScan to pull in all RowMappers and Query classes which are needed by Repositories.
 */
@JdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ".*(RowMapper|Queries|SqlLoader).*"
))
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUpBaseline() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("test-data-baseline.sql"));
        populator.execute(dataSource);
    }
}

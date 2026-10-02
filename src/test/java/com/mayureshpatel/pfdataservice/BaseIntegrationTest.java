package com.mayureshpatel.pfdataservice;

import com.mayureshpatel.pfdataservice.config.TestContainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Base class for integration tests that need a real, full Spring context backed by a real (Testcontainers-managed) PostgreSQL instance, under the {@code test} profile. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
public abstract class BaseIntegrationTest {
}

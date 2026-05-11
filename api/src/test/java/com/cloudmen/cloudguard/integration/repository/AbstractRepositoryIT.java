package com.cloudmen.cloudguard.integration.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for repository integration tests that hit a real MySQL instance
 * spun up by Testcontainers.
 *
 * <p>Uses {@link DataJpaTest} so only the JPA slice is loaded — the full application
 * context boots {@code Dotenv} and a number of external integrations (Google,
 * Mail, Teamleader, Supabase) that have no place in a repository test. Each test
 * still runs inside a transaction that is rolled back at the end, so state is
 * isolated across test methods.
 *
 * <p>{@code @AutoConfigureTestDatabase(replace = NONE)} disables Spring Boot's
 * default H2 replacement, and {@code @ServiceConnection} on the static
 * {@link MySQLContainer} binds the {@code DataSource} to the container without
 * needing manual {@code @DynamicPropertySource} wiring.
 *
 * <p>{@code @ActiveProfiles("test")} prevents {@code application-local.yml}
 * from pointing JPA at a developer's local MySQL on {@code localhost:3306}.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
abstract class AbstractRepositoryIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withReuse(true);
}

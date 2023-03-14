package io.github.edsoncunha.upgrade.takehome.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("it")
public class DatabaseIT {
    static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:12.5-alpine")
            .withUsername("test-user")
            .withPassword("test-pwd")
            .withDatabaseName("campingsite-demo")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        container.start();

        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.password=", container::getPassword);
        registry.add("spring.datasource.username=", container::getUsername);
    }

    @Test
    public void databseContextLoads() {

    }
}

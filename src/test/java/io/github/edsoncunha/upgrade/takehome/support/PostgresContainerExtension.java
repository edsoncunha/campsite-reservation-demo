package io.github.edsoncunha.upgrade.takehome.support;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.TimeZone;

public class PostgresContainerExtension implements BeforeAllCallback, AfterAllCallback {
    static PostgreSQLContainer<?> container;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        container = new PostgreSQLContainer<>("postgres:12.5-alpine")
                .withUsername("test-user")
                .withPassword("test-pwd")
                .withDatabaseName("campingsite-demo")
                .withEnv("TZ", TimeZone.getDefault().getID())
                .withEnv("PGTZ", TimeZone.getDefault().getID())
                .waitingFor(Wait.forListeningPort());



        container.start();

        System.setProperty("spring.datasource.url", container.getJdbcUrl());
        System.setProperty("spring.datasource.password", container.getPassword());
        System.setProperty("spring.datasource.username", container.getUsername());

        System.out.println("========================================================================================================");
        System.out.println("    Started Postgres test database ");
        System.out.println("    JDBC url: " + container.getJdbcUrl());
        System.out.println("    User: " + container.getUsername());
        System.out.println("    Password: " + container.getPassword());
        System.out.println("========================================================================================================");
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (container.isRunning()) {
            container.stop();
        }
    }
}

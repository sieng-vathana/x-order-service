package com.x.order.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Applies the schema changes Hibernate cannot make, such as new values in an existing enum column.
 *
 * <p>Hibernate owns the table structure ({@code ddl-auto: update}). Spring Boot would run Flyway
 * before Hibernate creates the tables, so its auto-run is off ({@code spring.flyway.enabled: false})
 * and Flyway runs here, after the entity manager factory is ready.
 */
@Component
@DependsOn("entityManagerFactory")
@RequiredArgsConstructor
public class DatabaseMigrations {

    /** Each service keeps its own history table because they all share one schema. */
    static final String HISTORY_TABLE = "flyway_schema_history_order";

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrations.class);

    private final DataSource dataSource;

    @PostConstruct
    void migrate() {
        MigrateResult result = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .table(HISTORY_TABLE)
                // Existing databases already hold tables, so their history starts at version 0.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
        log.info("Database migrations: {} applied", result.migrationsExecuted);
    }
}

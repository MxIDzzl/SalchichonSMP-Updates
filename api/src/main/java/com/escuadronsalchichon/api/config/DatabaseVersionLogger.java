package com.escuadronsalchichon.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Logs the connected database engine/version for deployment diagnostics.
 * This information is intentionally kept in server logs and is not exposed by
 * a public health endpoint.
 */
@Component
public class DatabaseVersionLogger {
    private static final Logger log = LoggerFactory.getLogger(DatabaseVersionLogger.class);

    private final DataSource dataSource;

    public DatabaseVersionLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseVersion() {
        try (Connection connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            log.info("Connected database engine: {} {}", metadata.getDatabaseProductName(),
                    metadata.getDatabaseProductVersion());
        } catch (SQLException exception) {
            log.warn("Could not read connected database engine/version after application startup.", exception);
        }
    }
}

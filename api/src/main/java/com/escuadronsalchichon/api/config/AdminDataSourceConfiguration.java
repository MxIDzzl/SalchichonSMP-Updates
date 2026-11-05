package com.escuadronsalchichon.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty(prefix = "salchichon.admin", name = "enabled", havingValue = "true")
public class AdminDataSourceConfiguration {
    private final String url;
    private final String username;
    private final String password;
    private HikariDataSource dataSource;

    public AdminDataSourceConfiguration(
            @Value("${SALCHICHON_ADMIN_DB_URL:}") String url,
            @Value("${SALCHICHON_ADMIN_DB_USER:}") String username,
            @Value("${SALCHICHON_ADMIN_DB_PASSWORD:}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Bean(name = "adminJdbcTemplate")
    public JdbcTemplate adminJdbcTemplate() {
        return new JdbcTemplate(adminDataSource());
    }

    @Bean(name = "adminTransactionManager")
    public PlatformTransactionManager adminTransactionManager() {
        return new DataSourceTransactionManager(adminDataSource());
    }

    private synchronized HikariDataSource adminDataSource() {
        if (dataSource != null) return dataSource;
        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Admin database credentials must be configured when admin routes are enabled");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("salchichon-admin");
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setReadOnly(false);
        config.setConnectionTimeout(10_000);
        config.setInitializationFailTimeout(10_000);
        dataSource = new HikariDataSource(config);
        return dataSource;
    }

    @PreDestroy
    public synchronized void close() {
        if (dataSource != null) dataSource.close();
    }
}

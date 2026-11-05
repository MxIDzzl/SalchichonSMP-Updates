package com.escuadronsalchichon.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "salchichon.admin", name = "enabled", havingValue = "true")
public class AdminDataSourceConfiguration {
    @Bean(name = "adminDataSource", destroyMethod = "close")
    public HikariDataSource adminDataSource(
            @Value("${SALCHICHON_ADMIN_DB_URL:}") String url,
            @Value("${SALCHICHON_ADMIN_DB_USER:}") String username,
            @Value("${SALCHICHON_ADMIN_DB_PASSWORD:}") String password) {
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
        return new HikariDataSource(config);
    }

    @Bean(name = "adminJdbcTemplate")
    public JdbcTemplate adminJdbcTemplate(DataSource adminDataSource) {
        return new JdbcTemplate(adminDataSource);
    }

    @Bean(name = "adminTransactionManager")
    public PlatformTransactionManager adminTransactionManager(DataSource adminDataSource) {
        return new DataSourceTransactionManager(adminDataSource);
    }
}

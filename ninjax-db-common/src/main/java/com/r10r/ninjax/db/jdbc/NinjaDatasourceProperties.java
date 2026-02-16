package com.r10r.ninjax.db.jdbc;

import java.util.Map;
import java.util.Optional;

public class NinjaDatasourceProperties {

    private final String name;
    private final Optional<String> driver;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    
    private final Optional<MigrationConfiguration> migrationConfiguration;
    
    // All properties under this namespace.
    // For instance application.datasource.my_datasource.hikari.maxConnections
    // This is useful to initialize other datasources...
    private final Map<String, String> properties;
    
    public NinjaDatasourceProperties(
            String name, 
            Optional<String> driver, 
            String jdbcUrl, 
            String username, 
            String password, 
            Optional<MigrationConfiguration> migrationConfiguration,
            Map<String, String> properties) {

        this.name = name;
        this.driver = driver;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.properties = properties;
        
        this.migrationConfiguration = migrationConfiguration;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDriver() {
        return driver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    public Optional<MigrationConfiguration> getMigrationConguration() {
        return this.migrationConfiguration;
    }
    
    public Map<String, String> getProperties() {
        return this.properties;
    }

    public static class MigrationConfiguration {
        
        private final String migrationUsername;
        private final String migrationPassword;

        public MigrationConfiguration(String migrationUsername, String migrationPassword) {
            this.migrationUsername = migrationUsername;
            this.migrationPassword = migrationPassword;
        }

        public String getMigrationUsername() {
            return migrationUsername;
        }

        public String getMigrationPassword() {
            return migrationPassword;
        }

    }
    
    
}
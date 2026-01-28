package org.ninjax.db.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ninjax.core.properties.NinjaProperties;
    
 public class NinjaDatasourcePropertiesExtractor {

    private final String DATASOURCE_PREFIX = "application.datasource";
    private final String DATASOURCE_URL = "url";
    private final String DATASOURCE_USERNAME = "username";
    private final String DATASOURCE_PASSWORD = "password";
    private final String DATASOURCE_DRIVER = "driver";

    private final String DATASOURCE_MIGRATION_ENABLED = "migration.enabled";
    private final String DATASOURCE_MIGRATION_USERNAME = "migration.username";
    private final String DATASOURCE_MIGRATION_PASSWORD = "migration.password";
    
    private final NinjaProperties ninjaProperties;
    

    public NinjaDatasourcePropertiesExtractor(NinjaProperties ninjaProperties) {
        this.ninjaProperties = ninjaProperties;
    }


    public NinjaDatasourcesProperties get() {
        List<NinjaDatasourceProperties> ninjaDatasourceConfigs = getDatasources(ninjaProperties);
        return new NinjaDatasourcesProperties(ninjaDatasourceConfigs);
    }

    private List<NinjaDatasourceProperties> getDatasources(NinjaProperties ninjaProperties) {
        Map<String, String> properties = ninjaProperties.getAllProperties();

        Set<String> datasourceNames = new HashSet<>();

        // filter datasources from application config
        for (Map.Entry<String, String> entrySet : properties.entrySet()) {
            if (((String) entrySet.getKey()).startsWith(DATASOURCE_PREFIX) && ((String) entrySet.getKey()).endsWith(DATASOURCE_URL)) {
                String withoutPrefix = ((String) entrySet.getKey()).split(DATASOURCE_PREFIX + ".")[1];
                String datasourceName = withoutPrefix.split("." + DATASOURCE_URL)[0];
                datasourceNames.add(datasourceName);
            }
        }

        // assemble the datasources in a nice way, so we can build datasources from it
        List<NinjaDatasourceProperties> ninjaDatasources = new ArrayList<>();
        for (String datasourceName : datasourceNames) {
            
            String name = datasourceName;
            var driverOpt = ninjaProperties.get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_DRIVER);
            
            // datasource
            String jdbcUrl = ninjaProperties.get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_URL).orElseThrow();
            String username = ninjaProperties.get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_USERNAME).orElseThrow();
            String password = ninjaProperties.get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_PASSWORD).orElseThrow();


            Optional<NinjaDatasourceProperties.MigrationConfiguration> migrationConfiguration = determineMigrationConfguration(datasourceName, username, password);

            Map<String, String> allProperties = getAllPropertiesOfThisDatasource(datasourceName, properties);
                    
            NinjaDatasourceProperties ninjaDatasourceConfig = new NinjaDatasourceProperties(
                    name,
                    driverOpt,
                    jdbcUrl,
                    username,
                    password,
                    migrationConfiguration,
                    allProperties
            );
            ninjaDatasources.add(ninjaDatasourceConfig);
        }

        return ninjaDatasources;

    }
    
    private Optional<NinjaDatasourceProperties.MigrationConfiguration> determineMigrationConfguration(String datasourceName, String username, String password) {
        boolean migrationEnabled = ninjaProperties
                .get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_MIGRATION_ENABLED)
                .map(v -> Boolean.valueOf(v))
                .orElse(Boolean.FALSE);

        Optional<NinjaDatasourceProperties.MigrationConfiguration> migrationConfiguration;
        if (migrationEnabled) {
            // fallback to username/password configured in datasource
            String migrationUsername = ninjaProperties.get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_MIGRATION_USERNAME).orElse(username);
            String migrationPassword = ninjaProperties.get(DATASOURCE_PREFIX + "." + datasourceName + "." + DATASOURCE_MIGRATION_PASSWORD).orElse(password);

            migrationConfiguration = Optional.of(new NinjaDatasourceProperties.MigrationConfiguration(migrationUsername, migrationPassword));
        } else {
            migrationConfiguration = Optional.empty();
        }
        
        return migrationConfiguration;
    }
    
    
    private Map<String, String> getAllPropertiesOfThisDatasource(String datasourceName, Map<String, String> properties) {
        
        Map<String, String> theseProperties = new HashMap();
        
        properties.entrySet().forEach(entry -> {
            String key = (String) entry.getKey();
            if (key.startsWith(DATASOURCE_PREFIX + "." + datasourceName)) {
                String keyWithoutDatasourceNamePrefix = key.split(DATASOURCE_PREFIX + "." + datasourceName + ".")[1];
                
                theseProperties.put(keyWithoutDatasourceNamePrefix, (String) entry.getValue());
            }
        });

        return Map.copyOf(theseProperties);
    }

}
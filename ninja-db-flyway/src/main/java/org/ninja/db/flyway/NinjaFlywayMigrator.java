package org.ninja.db.flyway;

import org.ninja.db.jdbc.NinjaDatasourceProperties;
import org.ninja.db.jdbc.NinjaDatasourcesProperties;
import org.flywaydb.core.Flyway;

public class NinjaFlywayMigrator {

    public NinjaFlywayMigrator(NinjaDatasourcesProperties ninjaDatasourceConfigs) {

        for (NinjaDatasourceProperties datasourceConfig : ninjaDatasourceConfigs.getDatasources()) {
            datasourceConfig.getMigrationConguration().ifPresent(migrationConfiguration -> {
                Flyway flyway = Flyway.configure()
                        .dataSource(datasourceConfig.getJdbcUrl(), migrationConfiguration.getMigrationUsername(), migrationConfiguration.getMigrationPassword())
                        .locations("classpath:migrations/" + datasourceConfig.getName() + "/")
                        .load();
                flyway.migrate();
            });
        }
    }

}
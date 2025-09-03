package org.ninjax.db.flyway;

import org.ninjax.db.jdbc.NinjaDatasourceProperties;
import org.ninjax.db.jdbc.NinjaDatasourcesProperties;
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

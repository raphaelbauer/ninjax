package org.r10r.ninjax.db.jdbc;

import java.util.List;


public class NinjaDatasourcesProperties {

    private final List<NinjaDatasourceProperties> ninjaDatasourceConfigs;

    public NinjaDatasourcesProperties(List<NinjaDatasourceProperties> datasources) {
        this.ninjaDatasourceConfigs = List.copyOf(datasources);
    }

    public List<NinjaDatasourceProperties> getDatasources() {
        return ninjaDatasourceConfigs;
    }
    
}
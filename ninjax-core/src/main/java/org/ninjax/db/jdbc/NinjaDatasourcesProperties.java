package org.ninjax.db.jdbc;

import com.google.common.collect.ImmutableList;
import java.util.List;


public class NinjaDatasourcesProperties {
    
    private final List<NinjaDatasourceProperties> ninjaDatasourceConfigs;
    
    public NinjaDatasourcesProperties(List<NinjaDatasourceProperties> datasources) {
        this.ninjaDatasourceConfigs = ImmutableList.copyOf(datasources);
    }

    public List<NinjaDatasourceProperties> getDatasources() {
        return ninjaDatasourceConfigs;
    }
    
}

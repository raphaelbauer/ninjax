package org.ninjax.db.jdbc;

import com.google.common.collect.ImmutableList;
import java.util.List;


public class NinjaDatasources {
    
    private final List<NinjaDatasource> ninjaDatasources;
    
    public NinjaDatasources(List<NinjaDatasource> datasources) {
        this.ninjaDatasources = ImmutableList.copyOf(datasources);
    }

    public List<NinjaDatasource> getDatasources() {
        return ninjaDatasources;
    }

    public NinjaDatasource getDatasource(String name) {
        return ninjaDatasources
                .stream()
                .filter(n -> n.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Datasource " + name + " not found. Make sure it is configured in application.conf."));
    }
    
}

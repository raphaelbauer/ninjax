package org.r10r.ninjax.db.jdbc;

import javax.sql.DataSource;

public class NinjaDatasource {
    private final String name;
    private final DataSource dataSource;
    
    public NinjaDatasource(String name, DataSource dataSource) {
        this.name = name;
        this.dataSource = dataSource;
    }
    
    public String getName() {
        return this.name;
    }
    
    public DataSource getDataSource() {
        return this.dataSource;
    }
}
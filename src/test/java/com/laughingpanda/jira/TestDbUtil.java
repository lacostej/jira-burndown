package com.laughingpanda.jira;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class TestDbUtil {
    private static Connection connection;
    
    public static DataSource getDataSource() {
        if (connection == null) {
            init();
        }
        executeClause("delete from version_workload_history");
        return new SingleConnectionDataSource(connection, false);
    }

    private static void init() {
        try {
            Class.forName("org.hsqldb.jdbcDriver" );
            connection = DriverManager.getConnection("jdbc:hsqldb:file:testdb", "sa", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // TODO Read create table sql from file
        executeClause("create table version_workload_history (versionID decimal(18,0), remainingTime decimal(18,0), totalTime decimal (18,0), remainingIssues decimal(18,0), totalIssues decimal(18,0), time timestamp);");
    }

    private static void executeClause(String clause) {
        DataSource dataSource = new SingleConnectionDataSource(connection, false);   
        new JdbcTemplate(dataSource).execute(clause);
    }
}

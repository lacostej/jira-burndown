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
        try {
            executeClause("delete from version_workload_history");
        } catch (Exception e) {
        }
        return new SingleConnectionDataSource(connection, true);
    }

    private static void init() {
        connection = new HSQLConnectionFactory().create();
    }

    private static void executeClause(String clause) {
        DataSource dataSource = new SingleConnectionDataSource(connection, true);
        new JdbcTemplate(dataSource).execute(clause);
    }

    private static interface ConnectionFactory {
        Connection create();
    }

    private static class HSQLConnectionFactory extends BasicConnectionFactory {

        public HSQLConnectionFactory() {
            super("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb", "sa", "");
        }

    }

    private static class MysqlConnectionFactory extends BasicConnectionFactory {

        public MysqlConnectionFactory() {
            super("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/jiradb", "jirauser", "jirauser");
        }

    }

    private static class SQLServerConnectionFactory extends BasicConnectionFactory {
        public SQLServerConnectionFactory() {
            super("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://localhost:4533", "test", "test");
        }
    }
    
    private static class PostgreConnectionFactory extends BasicConnectionFactory {
        public PostgreConnectionFactory() {
            super("org.postgresql.Driver", "jdbc:postgresql://localhost:5432", "postgres", "postgres");
        }
    }


    private static class BasicConnectionFactory {

        private final String jdbcUrl;
        private final String user;
        private final String password;

        public BasicConnectionFactory(String driver, String jdbcUrl, String user, String password) {
            try {
                Class.forName(driver);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.password = password;

        }

        public Connection create() {
            try {
                return DriverManager.getConnection(jdbcUrl, user, password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}

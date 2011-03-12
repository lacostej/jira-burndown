package com.laughingpanda.jira;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.sql.DataSource;

import org.apache.log4j.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import com.atlassian.jira.util.IOUtil;

class QueryHelper {

    private final Category log = Category.getInstance(QueryHelper.class);
    private String databaseName = "DEFAULT";
    private ResourceBundle sqlBundle;
    private JdbcTemplate template;

    public QueryHelper(DataSource datasource) {
        template = new JdbcTemplate(datasource);
        resolveDatabaseName(datasource);
        resolveDatabaseClauseBundle();
        runCreateScriptIfTablesDoNotExist();
    }

    public List query(String queryName, RowMapper mapper, Object... params) {
        String clause = getClauseByName(queryName);
        if (clause == null || clause.length() == 0) return Collections.emptyList();
        return template.query(clause, params, mapper);
    }

    public void update(String updateName, Object... params) {
        String clause = getClauseByName(updateName);
        if (clause == null || clause.length() == 0) return;
        if (params.length == 0) {
            template.execute(clause);
            return;
        }
        template.update(clause, params);
    }

    private void resolveDatabaseClauseBundle() {
        sqlBundle = ResourceBundle.getBundle(databaseName + ".database_queries");
    }

    private void resolveDatabaseName(DataSource datasource) {
        try {
            databaseName = (String) JdbcUtils.extractDatabaseMetaData(datasource, "getDatabaseProductName");
            databaseName = databaseName.replaceAll(" ", "");
            log.info("Resolved database name '" + databaseName + "'.");
        } catch (MetaDataAccessException e) {
            log.warn("Could not access database product name; using default fetching database clauses.", e);
        }
    }

    private void runCreateScriptIfTablesDoNotExist() {
        if (isSqlQueryPossible(getClauseByName("DATABASE_OK"))) return;
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(databaseName + "/create.sql");
        if (resourceAsStream == null) throw new IllegalStateException("Cannot find resource. " + databaseName + "/create.sql");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        try {
            while (reader.ready()) {
                template.execute(reader.readLine());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.shutdownReader(reader);
        }
    }

    private boolean isSqlQueryPossible(String testQuery) {
        try {
            template.execute(testQuery);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String getClauseByName(String clauseName) {
        return sqlBundle.getString(clauseName);
    }

}
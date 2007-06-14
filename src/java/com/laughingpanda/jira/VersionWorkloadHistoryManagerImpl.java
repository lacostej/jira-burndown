/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.log4j.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import com.laughingpanda.jira.VersionWorkloadHistoryManager;
import com.laughingpanda.jira.VersionWorkloadHistoryPoint;

/**
 * @author Jukka Lindstrom
 * @author Markus Hjort
 */
public class VersionWorkloadHistoryManagerImpl implements VersionWorkloadHistoryManager {

    private String databaseName = "DEFAULT";
    private ResourceBundle sqlBundle;
    private JdbcTemplate template;
    private final Category log = Category.getInstance(VersionWorkloadHistoryManagerImpl.class);

    Map<Long, Pair> startPointsForVersions = new HashMap<Long, Pair>();
    Map<Long, Pair> lastPointsForVersions = new HashMap<Long, Pair>();

    public static DataSource getJiraJNDIDataSource() {
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) { throw new RuntimeException("Init: Cannot get Initial Context"); }
            return (DataSource) initialContext.lookup("java:comp/env/jdbc/JiraDS");
        } catch (NamingException ex) {
            throw new RuntimeException("Init: Cannot get connection.", ex);
        }
    }

    public VersionWorkloadHistoryManagerImpl() {
        this(getJiraJNDIDataSource());
    }

    public VersionWorkloadHistoryManagerImpl(DataSource datasource) {
        template = new JdbcTemplate(datasource);
        resolveDatabaseName(datasource);
        resolveDatabaseQueries();
        createTableIfNotExists();
    }

    private void resolveDatabaseQueries() {
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

    private void createTableIfNotExists() {
        if (sqlQueryPossible(getClauseByName("DATABASE_OK"))) return;
        InputStream resourceAsStream = ClassLoader.getSystemResourceAsStream(databaseName + "/create.sql");
        if (resourceAsStream == null) throw new IllegalStateException("Cannot find resource." + databaseName + "/create.sql");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        try {
            while (reader.ready()) {
                template.execute(reader.readLine());
            }
        } catch (IOException e) {
            throw new UnsupportedOperationException("Catch not implemented.");

        }
    }

    private boolean sqlQueryPossible(String testQuery) {
        try {
            template.execute(testQuery);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static final RowMapper mapper = new RowMapper() {
        public Object mapRow(ResultSet rs, int row) throws SQLException {
            VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
            point.measureTime = rs.getTimestamp("time");
            point.remainingEffort = rs.getLong("remainingTime");
            point.remainingIssues = rs.getLong("remainingIssues");
            point.totalEffort = rs.getLong("totalTime");
            point.totalIssues = rs.getLong("totalIssues");
            point.versionId = rs.getLong("versionId");
            point.type = rs.getLong("type");
            return point;
        }
    };

    public synchronized void storeWorkload(VersionWorkloadHistoryPoint point) {
        log.debug("Storing workload point.");
        if (!lastPointsForVersions.containsKey(point.versionId)) {
            lastPointsForVersions.put(point.versionId, new Pair<VersionWorkloadHistoryPoint>(point, point));
        }

        Pair<VersionWorkloadHistoryPoint> range = lastPointsForVersions.get(point.versionId);
        if (!range.isStartOnly() && measurementsEqual(point, range.end)) {
            template.update(getClauseByName("UPDATE"), new Object[] { point.measureTime, point.versionId, range.end.measureTime });
        } else {
            insert(point);
            if (!range.isStartOnly()) range.start = point;
        }
        range.end = point;
        template.update("COMMIT");
    }

    private void insert(VersionWorkloadHistoryPoint point) {
        template.update(getClauseByName("INSERT"), new Object[] { point.versionId, point.measureTime, point.remainingEffort, point.remainingIssues, point.totalEffort, point.totalIssues, point.type });
    }

    private boolean measurementsEqual(VersionWorkloadHistoryPoint point, VersionWorkloadHistoryPoint last) {
        return new EqualsBuilder().append(point.remainingIssues, last.remainingIssues).append(point.remainingEffort, last.remainingEffort).append(point.totalIssues, last.totalIssues).append(point.totalEffort, last.totalEffort).append(point.type, last.type).isEquals();
    }

    public List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate(Long versionId, Long type, final Date startDate) {
        log.debug("Retrieving workload for version '" + versionId + "' with startDate '" + startDate + "'.");
        List<VersionWorkloadHistoryPoint> all = template.query(getClauseByName("SELECT_VERSION_DATA"), new Object[] { versionId, type }, mapper);

        Predicate predicate = new Predicate() {
            public boolean evaluate(Object arg0) {
                return (((VersionWorkloadHistoryPoint) arg0).measureTime.before(startDate));
            }
        };

        List<VersionWorkloadHistoryPoint> points = new ArrayList<VersionWorkloadHistoryPoint>();
        CollectionUtils.select(all, PredicateUtils.notPredicate(predicate), points);

        Collection<VersionWorkloadHistoryPoint> beforeCutOff = CollectionUtils.select(all, predicate);
        if (beforeCutOff.size() > 0) {
            points.add(0, Collections.max(beforeCutOff));
        }
        return Collections.unmodifiableList(points);
    }

    public String getClauseByName(String name) {
        return sqlBundle.getString(name);
    }

}

class Pair<T> {

    public Pair(T start, T end) {
        this.start = start;
        this.end = end;
    }

    T start;
    T end;

    boolean isStartOnly() {
        return start.equals(end);
    }
}
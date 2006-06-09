/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/**
 * @author Jukka Lindstrom
 * @author Markus Hjort
 */
public class VersionWorkloadHistoryManagerImpl implements VersionWorkloadHistoryManager {
    
    private JdbcTemplate template;
    private final Category log = Category.getInstance(VersionWorkloadHistoryManagerImpl.class);

    Map<Long, Pair> startPointsForVersions = new HashMap<Long, Pair>();
    Map<Long, Pair> lastPointsForVersions = new HashMap<Long, Pair>();

    public static DataSource getJNDIDataSource() {
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) { throw new RuntimeException("Init: Cannot get Initial Context"); }
            return (DataSource) initialContext.lookup("java:comp/env/jdbc/JiraDS");
        } catch (NamingException ex) {
            throw new RuntimeException("Init: Cannot get connection.", ex);
        }
    }

    public VersionWorkloadHistoryManagerImpl() {
        this(getJNDIDataSource());
    }

    public VersionWorkloadHistoryManagerImpl(DataSource datasource) {
        template = new JdbcTemplate(datasource);
        
        // we'd really need some other kind of way to update the schemas
        createTablesIfNotExists();
        update1();
    }

    private void update1() {
        runUpdateIfNeeded(
                "SELECT type FROM version_workload_history",
                "ALTER TABLE version_workload_history ADD COLUMN type DECIMAL(18,0) DEFAULT -1"
        );        
    }

    private void createTablesIfNotExists() {
        runUpdateIfNeeded(
                "SELECT * FROM version_workload_history", 
                "CREATE TABLE version_workload_history (versionID DECIMAL(18,0), remainingTime DECIMAL(18,0), totalTime DECIMAL(18,0), remainingIssues DECIMAL(18,0), totalIssues DECIMAL(18,0), time TIMESTAMP)");
    }

    private void runUpdateIfNeeded(String testQuery, String updateQuery) {
        try {
            template.execute(testQuery);
        } catch (Exception e) {
            try {
                template.execute(updateQuery);
            } catch (Exception ex) {
                log.error("Update '" + updateQuery + "' failed.", e);
            };
        }
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
            template.update("UPDATE version_workload_history SET time = ? WHERE versionId = ? AND time = ?", new Object[] { point.measureTime, point.versionId, range.end.measureTime });
        } else {
            insert(point);
            if (!range.isStartOnly()) range.start = point;
        }
        range.end = point;
        template.update("COMMIT");
    }

    private void insert(VersionWorkloadHistoryPoint point) {
        template.update("INSERT INTO version_workload_history (versionId, time, remainingTime, remainingIssues, totalTime, totalIssues, type) VALUES (?,?,?,?,?,?,?)", new Object[] { point.versionId, point.measureTime, point.remainingEffort, point.remainingIssues, point.totalEffort, point.totalIssues, point.type });
    }

    private boolean measurementsEqual(VersionWorkloadHistoryPoint point, VersionWorkloadHistoryPoint last) {
        return new EqualsBuilder().append(point.remainingIssues, last.remainingIssues).append(point.remainingEffort, last.remainingEffort).append(point.totalIssues, last.totalIssues).append(point.totalEffort, last.totalEffort).append(point.type, last.type).isEquals();
    }


    public List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate(Long versionId, Long type, final Date startDate) {
        log.debug("Retrieving workload for version '" + versionId + "' with startDate '" + startDate + "'.");
        List<VersionWorkloadHistoryPoint> all = template.query("SELECT * FROM version_workload_history WHERE versionId = ? AND type = ? ORDER BY time", new Object[] { versionId, type }, mapper);

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
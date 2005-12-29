/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Jukka Lindstrom
 * @author Markus Hjort
 */
public class VersionWorkloadHistoryManagerImpl implements
        VersionWorkloadHistoryManager {

    private JdbcTemplate template;

    public static DataSource getJNDIDataSource() {
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) {
                throw new RuntimeException("Init: Cannot get Initial Context");
            }
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
    }

    static final RowMapper mapper = new RowMapper() {
        public Object mapRow(ResultSet rs, int row) throws SQLException {
            VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
            point.measureTime = rs.getTimestamp("time");
            point.remainingTime = rs.getLong("remainingTime");
            point.remainingIssues = rs.getLong("remainingIssues");
            point.totalTime = rs.getLong("totalTime");
            point.totalIssues = rs.getLong("totalIssues");
            point.versionId = rs.getLong("versionId");
            return point;
        }
    };

    public void storeWorkload(VersionWorkloadHistoryPoint point) {
        log.debug("Storing workload point.");
        template.update(
                "INSERT INTO version_workload_history (versionId, time, remainingTime, remainingIssues, totalTime, totalIssues) VALUES (?,?,?,?,?,?)", 
                new Object[] { point.versionId, point.measureTime, point.remainingTime, point.remainingIssues, point.totalTime, point.totalIssues }
        );
        template.update("COMMIT");
    }
    
    private final Category log = Category.getInstance(VersionWorkloadHistoryManagerImpl.class);

    public List<VersionWorkloadHistoryPoint> getWorkload(Long versionId, Date startDate) {
        log.debug("Retrieving workload for version '" + versionId + "' with startDate '" + startDate + "'.");
        
        // Note! We use two sql clauses here but this can be implemented using single sql query with subselects.
        // However older versions of MySQL (prior 4.1) do not support this feature.  
        Date latestHistoryPointBeforeStartDate = 
            (Date) template.queryForObject("SELECT MAX(time) FROM version_workload_history WHERE versionId = ? AND time < ? ",
            new Object[] {versionId, startDate}, Date.class);
        
        return template.query(
                "SELECT * FROM version_workload_history WHERE versionId = ? AND (time >= ? OR time = ?)",
                new Object[] { versionId, startDate, latestHistoryPointBeforeStartDate }, mapper);
    }

}

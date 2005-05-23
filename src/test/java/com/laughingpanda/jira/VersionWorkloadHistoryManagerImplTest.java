/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import junit.framework.TestCase;

import com.mockobjects.sql.MockConnection;
import com.mockobjects.sql.MockDataSource;
import com.mockobjects.sql.MockDatabaseMetaData;

public class VersionWorkloadHistoryManagerImplTest extends TestCase {

    MockDataSource datasource = new MockDataSource();
    MockConnection connection = new MockConnection();
    MockDatabaseMetaData metadata = new MockDatabaseMetaData() {
        public String getDatabaseProductName() {
            return "Mysql";
        }
    };

    public void testNothing() {}
    /*
    public void testSave() {
        datasource.setupConnection(connection);
        connection.setupMetaData(metadata);
        metadata.setupDriverName("org");
        
        VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
        point.time = new Date();
        point.versionId = new Long(1);
        point.remainingIssues = 1;
        point.remainingTime = 3600;
        
        VersionWorkloadHistoryManagerImpl impl = new VersionWorkloadHistoryManagerImpl(datasource);
        impl.storeWorkload(point);
    }
    */
}

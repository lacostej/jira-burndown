
package com.laughingpanda.jira;

import java.util.Date;

import junit.framework.TestCase;

public class VersionWorkloadHistoryPointTest extends TestCase {
    
    public void testAddingTwo() throws Exception {
        VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
        point.measureTime = new Date();
        point.remainingEffort = 10L;
        point.totalEffort = 100L;
        point.remainingIssues = 10L;
        point.totalIssues = 100L;
    }

}

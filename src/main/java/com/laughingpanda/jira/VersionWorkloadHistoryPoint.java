/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.util.Date;

public class VersionWorkloadHistoryPoint implements Comparable<VersionWorkloadHistoryPoint>{
    
    public Long versionId;
    public Date measureTime;
    public long remainingTime;
    public long remainingIssues;
    public long totalIssues;
    public long totalTime;
 
    public String toString() {
        return "Version:" + versionId + ", " + measureTime + ", time: " + remainingTime + "/" + totalTime + ", issues: " + remainingIssues + "/" + totalIssues;  
    }

    public int compareTo(VersionWorkloadHistoryPoint o) {
        return (int) ((measureTime.getTime() - o.measureTime.getTime()) / 1000); 
    }
    
}

/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.util.Date;

public class VersionWorkloadHistoryPoint implements Comparable<VersionWorkloadHistoryPoint>{
    
    public Long versionId;
    public Date measureTime = new Date();
    public long remainingEffort;
    public long remainingIssues;
    public long totalIssues;
    public long totalEffort;
    public Long type;
 
    public String toString() {
        return "Version:" + versionId + ", " + measureTime + ", time: " + remainingEffort + "/" + totalEffort + ", issues: " + remainingIssues + "/" + totalIssues + " type: "+ type;  
    }    

    public int compareTo(VersionWorkloadHistoryPoint o) {
        return (int) ((measureTime.getTime() - o.measureTime.getTime()) / 1000); 
    }

    public void add(VersionWorkloadHistoryPoint point) {
        if (this.versionId != point.versionId && point.versionId != null) throw new IllegalArgumentException("Cannot add together issues of different versions.");
        this.remainingEffort += point.remainingEffort;
        this.remainingIssues += point.remainingIssues;
        this.totalEffort += point.totalEffort;
        this.totalIssues += point.totalIssues;
        if (this.type == null) type = point.type;
        if (this.type != point.type) throw new IllegalArgumentException("Cannot add together issues of different types.");
    }
    
}

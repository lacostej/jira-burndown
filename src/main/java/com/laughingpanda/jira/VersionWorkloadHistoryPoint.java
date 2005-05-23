/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.util.Date;

public class VersionWorkloadHistoryPoint {
    
    public Long versionId;
    public Date measureTime;
    public long remainingTime;
    public long remainingIssues;
    public long totalIssues;
    public long totalTime;
    
}

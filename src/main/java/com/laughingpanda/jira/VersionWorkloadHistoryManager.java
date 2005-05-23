/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import java.util.List;

public interface VersionWorkloadHistoryManager {

    public List<VersionWorkloadHistoryPoint> getWorkload(Long versionId);
    
    public void storeWorkload(VersionWorkloadHistoryPoint point);

}

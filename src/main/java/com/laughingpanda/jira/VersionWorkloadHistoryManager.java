/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import com.laughingpanda.jira.VersionWorkloadHistoryPoint;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author Jukka Lindstrom
 * @author Markus Hjort
 */
public interface VersionWorkloadHistoryManager {

    public List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate(Long versionId, Long type, Date startDate);
    
    public void storeWorkload(VersionWorkloadHistoryPoint point);

}

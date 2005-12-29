/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author Jukka Lindstrom
 * @author Markus Hjort
 */
public interface VersionWorkloadHistoryManager {

    public List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate(Long versionId, Date startDate);
    
    public void storeWorkload(VersionWorkloadHistoryPoint point);

}

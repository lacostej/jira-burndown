package com.laughingpanda.jira;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
/**
 * @author Markus Hjort
 */
public class VersionWorkloadHistoryManagerImplTest extends TestCase {
    
    private static final Long VERSION_ID = new Long(1);

    private SimpleDateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    
    private VersionWorkloadHistoryManagerImpl manager;

    @Override
    protected void setUp() throws Exception {
        manager = new VersionWorkloadHistoryManagerImpl(TestDbUtil.getDataSource());   
    }
    
    public void testStoringAndLoadingOne() throws Exception {
        VersionWorkloadHistoryPoint historyPoint = createHistoryPoint(new Date());
        manager.storeWorkload(historyPoint);
        
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkload(VERSION_ID, new Date());
        // TODO Test using WorkloadHistoryPoint.equals
        assertEquals(1, workloads.size());
        assertEquals(10, workloads.get(0).remainingTime);
    }
    
    public void testStoringAndLoadingUsingStartDate() throws Exception {
        storeWorkloadHistoryPoint("2005-01-01");
        storeWorkloadHistoryPoint("2005-02-02");
        storeWorkloadHistoryPoint("2005-03-03");
        
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkload(VERSION_ID, isoDateFormatter.parse("2005-02-15"));
        assertEquals(2, workloads.size());
        assertEquals("2005-02-02", isoDateFormatter.format(workloads.get(0).measureTime));
        assertEquals("2005-03-03", isoDateFormatter.format(workloads.get(1).measureTime));
    }

    public void testWorkloadNotFound() throws Exception {
        assertEquals(0, manager.getWorkload(VERSION_ID, new Date()).size());
    }
    
    private VersionWorkloadHistoryPoint createHistoryPoint(Date measureTime) {
        VersionWorkloadHistoryPoint historyPoint = new VersionWorkloadHistoryPoint();
        historyPoint.measureTime = measureTime;
        historyPoint.remainingIssues = 2;
        historyPoint.remainingTime = 10;
        historyPoint.totalIssues = 3;
        historyPoint.versionId = VERSION_ID;
        historyPoint.totalTime = 20;
        return historyPoint;
    }
    
    private void storeWorkloadHistoryPoint(String measureDate) throws ParseException {
        manager.storeWorkload(createHistoryPoint(isoDateFormatter.parse(measureDate)));
    }
}

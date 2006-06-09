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
        VersionWorkloadHistoryPoint historyPoint = createHistoryPoint(new Date(), 10, 20);
        manager.storeWorkload(historyPoint);
        
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkloadStartingFromMaxDateBeforeGivenDate(VERSION_ID, -1L, new Date());
        // TODO Test using WorkloadHistoryPoint.equals
        assertEquals(1, workloads.size());
        assertEquals(10, workloads.get(0).remainingEffort);
    }
    
    public void testStoringAndLoadingWhenThereIsPointsBeforeStartDate() throws Exception {
        storeWorkloadHistoryPoint("2005-01-01", 20, 20);
        storeWorkloadHistoryPoint("2005-02-02", 15, 20);
        storeWorkloadHistoryPoint("2005-03-03", 10, 20);
        
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkloadStartingFromMaxDateBeforeGivenDate(VERSION_ID, -1L, isoDateFormatter.parse("2005-02-15"));
        assertEquals(2, workloads.size());
        assertEquals("2005-02-02", isoDateFormatter.format(workloads.get(0).measureTime));
        assertEquals("2005-03-03", isoDateFormatter.format(workloads.get(1).measureTime));
    }
    
    public void testWhenStartDateBeforePoints() throws Exception {
        storeWorkloadHistoryPoint("2005-01-01", 10, 20);
        
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkloadStartingFromMaxDateBeforeGivenDate(VERSION_ID, -1L, isoDateFormatter.parse("2004-02-15"));
        assertEquals(1, workloads.size());
    }

    public void testWorkloadNotFound() throws Exception {
        assertEquals(0, manager.getWorkloadStartingFromMaxDateBeforeGivenDate(VERSION_ID, -1L, new Date()).size());
    }

    public void testNoNewPointsInsertedIfThereIsNoChange() throws Exception {
        storeWorkloadHistoryPoint("2005-02-15", 10, 20);
        storeWorkloadHistoryPoint("2005-02-16", 10, 20);
        storeWorkloadHistoryPoint("2005-02-17", 10, 20);
        storeWorkloadHistoryPoint("2005-02-18", 10, 20);
        storeWorkloadHistoryPoint("2005-02-19", 10, 20);
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkloadStartingFromMaxDateBeforeGivenDate(VERSION_ID, -1L, isoDateFormatter.parse("2005-02-15"));
        assertEquals(2, workloads.size());
        assertEquals("2005-02-15", isoDateFormatter.format(workloads.get(0).measureTime));
        assertEquals("2005-02-19", isoDateFormatter.format(workloads.get(1).measureTime));        
    }
    
    public void testIfThereIsChangeTheEndPointsaArePreserved() throws Exception {
        System.out.println("---");
        storeWorkloadHistoryPoint("2005-02-15", 20, 20);
        storeWorkloadHistoryPoint("2005-02-16", 20, 20);
        storeWorkloadHistoryPoint("2005-02-17", 20, 20);
        storeWorkloadHistoryPoint("2005-02-18", 20, 20);
        storeWorkloadHistoryPoint("2005-02-19", 20, 20);
        storeWorkloadHistoryPoint("2005-02-20", 15, 20);
        storeWorkloadHistoryPoint("2005-02-21", 15, 20);
        storeWorkloadHistoryPoint("2005-02-22", 15, 20);
        storeWorkloadHistoryPoint("2005-02-23", 10, 20);
        storeWorkloadHistoryPoint("2005-02-24", 10, 20);
        List<VersionWorkloadHistoryPoint> workloads = manager.getWorkloadStartingFromMaxDateBeforeGivenDate(VERSION_ID, -1L, isoDateFormatter.parse("2005-02-15"));
        assertEquals("wrong number of results : " + workloads, 6, workloads.size());
        assertEquals("2005-02-15", isoDateFormatter.format(workloads.get(0).measureTime));
        assertEquals("2005-02-19", isoDateFormatter.format(workloads.get(1).measureTime));        
        assertEquals("2005-02-20", isoDateFormatter.format(workloads.get(2).measureTime));
        assertEquals("2005-02-22", isoDateFormatter.format(workloads.get(3).measureTime));
        assertEquals("2005-02-23", isoDateFormatter.format(workloads.get(4).measureTime));
        assertEquals("2005-02-24", isoDateFormatter.format(workloads.get(5).measureTime));        
        
        
    }

    
    private VersionWorkloadHistoryPoint createHistoryPoint(Date measureTime, int remainingTime, int totalTime) {
        VersionWorkloadHistoryPoint historyPoint = new VersionWorkloadHistoryPoint();
        historyPoint.measureTime = measureTime;
        historyPoint.remainingIssues = 2;
        historyPoint.remainingEffort = remainingTime;
        historyPoint.totalIssues = 3;
        historyPoint.versionId = VERSION_ID;
        historyPoint.totalEffort = totalTime;
        historyPoint.type = -1L;
        return historyPoint;
    }
    
    private void storeWorkloadHistoryPoint(String measureDate, int remainingTime, int totalTime) throws ParseException {
        manager.storeWorkload(createHistoryPoint(isoDateFormatter.parse(measureDate), remainingTime, totalTime));
    }
    
}

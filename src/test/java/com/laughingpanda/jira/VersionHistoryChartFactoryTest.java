
package com.laughingpanda.jira;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import junit.framework.TestCase;

import org.jfree.chart.JFreeChart;

import com.atlassian.jira.project.version.Version;
import com.laughingpanda.mocked.MockFactory;

public class VersionHistoryChartFactoryTest extends TestCase {       
    
    static private final SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
    VersionHistoryChartFactory factory;
    private MockVersion version;
    
    static private Date parse(String date) {
        try {
            return format.parse(date);
        } catch (ParseException e) {
            throw new UnsupportedOperationException("Catch not implemented.");           
        }
    }
    
    static class MockManager implements VersionWorkloadHistoryManager {  
        public MockManager() {}

        public List<VersionWorkloadHistoryPoint> getWorkload(Long versionId) {
            List<VersionWorkloadHistoryPoint> list = new LinkedList<VersionWorkloadHistoryPoint>();
            list.add(makePoint(parse("06:00 01.01.2005"), 0, 0, 0, 0));
            list.add(makePoint(parse("18:00 18.01.2005"), 0, 0, 0, 0));
            return list;
        }

        private VersionWorkloadHistoryPoint makePoint(Date time, long remIssues, long totIssues, long remTime, long totTime) {
            VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
            point.measureTime = time;
            point.remainingIssues = remIssues;
            point.remainingTime = remTime;
            point.totalIssues = totIssues;
            point.totalTime = totTime;
            return point;
        }

        public void storeWorkload(VersionWorkloadHistoryPoint point) {
            throw new UnsupportedOperationException("Method not implemented.");
        }
        
    }
    
    static abstract class MockVersion implements Version {
        public MockVersion() {}

        public Long getId() {
            return new Long(100);
        };        
        
        public boolean isArchived() {
            return false;
        }
        
        public boolean isReleased() {
            return false;
        }
        
        public String getName() {
            return "name";
        }
        
        public Date releaseDate;         
        public Date getReleaseDate() {
            return releaseDate; 
        }
        
    }
    
    public void setUp() throws Exception {
        super.setUp();
        VersionWorkloadHistoryManager manager = (VersionWorkloadHistoryManager) MockFactory.makeMock(MockManager.class);
        factory = new VersionHistoryChartFactory(manager);
        version = (MockVersion) MockFactory.makeMock(MockVersion.class);   
    }
    
    public void testSimple() { 
        version.releaseDate = parse("00:00 01.02.2005");
        JFreeChart chart = factory.makeChart(version);
        assertEquals("Chart Title should be picked from the version", "name", chart.getTitle().getText());
        assertEquals("Expected the background color to be normal for non released/archived.", Color.WHITE, chart.getPlot().getBackgroundPaint());
        assertTrue("Release date should be inside the shown range. " + chart.getXYPlot().getDomainAxis().getRange(), version.releaseDate.getTime() < chart.getXYPlot().getDomainAxis().getRange().getUpperBound());       
    }
    
    public void testWithNullReleaseDate() {
        version.releaseDate = null;
        JFreeChart chart = factory.makeChart(version);
    }
}


package com.laughingpanda.jira;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.jfree.chart.JFreeChart;

import com.atlassian.jira.project.version.Version;
import com.laughingpanda.mocked.MockFactory;

public class VersionHistoryChartFactoryTest extends TestCase {       
    
    static private final SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
    static private final Color RELEASE_COLOR = new Color(224,224,224);

    private VersionHistoryChartFactory factory;
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
        
        public boolean archived;
        public boolean isArchived() {
            return archived;
        }
        
        public boolean released;
        public boolean isReleased() {
            return released;
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
        VersionWorkloadHistoryManager manager = new MockManager();
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
    
    public void testArchivedBackground() {
        version.archived = true;
        JFreeChart chart = factory.makeChart(version);        
        assertEquals("Archived should have gray background.", RELEASE_COLOR, chart.getPlot().getBackgroundPaint());
        assertTrue("Archived should be included in title", chart.getTitle().getText().indexOf("[archived]") != -1);
    }
    
    public void testReleasedBackground() {
        version.released = true;
        JFreeChart chart = factory.makeChart(version);        
        assertEquals("Released should have gray background.", RELEASE_COLOR, chart.getPlot().getBackgroundPaint());
        assertTrue("Released should be included in title", chart.getTitle().getText().indexOf("[released]") != -1);
    }
}

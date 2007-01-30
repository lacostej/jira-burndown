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
import com.laughingpanda.mocked.Recorder;

public class VersionHistoryChartFactoryTest extends TestCase {

    static private final SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
    static private final Color RELEASE_COLOR = new Color(224, 224, 224);

    private VersionHistoryChartFactory factory;
    private MockVersion version;
    private VersionWorkloadHistoryManager manager;

    static private Date parse(String date) {
        try {
            return format.parse(date);
        } catch (ParseException e) {
            throw new UnsupportedOperationException("Catch not implemented.");
        }
    }

    static class MockManager implements VersionWorkloadHistoryManager {
        public MockManager() {
        }

        public List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate(Long versionId, Long type, Date startDate) {
            List<VersionWorkloadHistoryPoint> list = new LinkedList<VersionWorkloadHistoryPoint>();
            list.add(makePoint(parse("06:00 01.01.2005"), 0, 0, 3600, 36000));
            list.add(makePoint(parse("18:00 18.01.2005"), 0, 0, 3600, 36000));
            return list;
        }

        private VersionWorkloadHistoryPoint makePoint(Date time, long remIssues, long totIssues, long remTime, long totTime) {
            VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
            point.measureTime = time;
            point.remainingIssues = remIssues;
            point.remainingEffort = remTime;
            point.totalIssues = totIssues;
            point.totalEffort = totTime;
            point.type = -1L;
            return point;
        }

        public void storeWorkload(VersionWorkloadHistoryPoint point) {
            throw new UnsupportedOperationException("Method not implemented.");
        }

    }

    static abstract class MockVersion implements Version {
        private static final Long VERSION_ID = new Long(100);

        public MockVersion() {
        }

        public Long getId() {
            return VERSION_ID;
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
        manager = new MockManager();
        factory = new VersionHistoryChartFactory(manager);
        version = (MockVersion) MockFactory.makeMock(MockVersion.class);
    }

    public void testSimple() {
        version.releaseDate = parse("00:00 01.02.2005");
        JFreeChart chart = factory.makeChart(version, -1L, new Date());
        assertEquals("Chart Title should be picked from the version", "name", chart.getTitle().getText());
        assertEquals("Expected the background color to be normal for non released/archived.", Color.WHITE, chart.getPlot().getBackgroundPaint());
        assertTrue("Release date should be inside the shown range. " + chart.getXYPlot().getDomainAxis().getRange(), version.releaseDate.getTime() < chart.getXYPlot().getDomainAxis().getRange().getUpperBound());

        assertEquals(Color.RED, chart.getXYPlot().getRenderer().getSeriesPaint(0));
        assertEquals("first series should be remaining time", 1, chart.getXYPlot().getDataset().getYValue(0, 0), 0d);

        assertEquals(Color.BLUE, chart.getXYPlot().getRenderer().getSeriesPaint(1));
        assertEquals("second series should be total time", 10, chart.getXYPlot().getDataset().getYValue(1, 0), 0d);

        return;
    }

    public void testCallsHistoryManagerCorrectly() throws Exception {
        VersionWorkloadHistoryManager observedManager = (VersionWorkloadHistoryManager) Recorder.observe(manager);

        version.releaseDate = parse("00:00 01.02.2006");
        JFreeChart chart = new VersionHistoryChartFactory(observedManager).makeChart(version, -1L, parse("00:00 01.02.2004"));

        Recorder.startAssertion(observedManager);
        observedManager.getWorkloadStartingFromMaxDateBeforeGivenDate(new Long(100), -1L, parse("00:00 01.02.2004"));
        Recorder.endAssertion(observedManager);

        return;
    }

    public void testWithNullReleaseDate() {
        version.releaseDate = null;
        JFreeChart chart = factory.makeChart(version, -1L, new Date());

    }

    public void testArchivedBackground() {
        version.archived = true;
        JFreeChart chart = factory.makeChart(version, -1L, new Date());
        assertEquals("Archived should have gray background.", RELEASE_COLOR, chart.getPlot().getBackgroundPaint());
        assertTrue("Archived should be included in title", chart.getTitle().getText().indexOf("[archived]") != -1);
    }

    public void testReleasedBackground() {
        version.released = true;
        JFreeChart chart = factory.makeChart(version, -1L, new Date());
        assertEquals("Released should have gray background.", RELEASE_COLOR, chart.getPlot().getBackgroundPaint());
        assertTrue("Released should be included in title", chart.getTitle().getText().indexOf("[released]") != -1);
    }
}

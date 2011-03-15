package com.laughingpanda.jira;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.jira.ComponentManager;
import junit.framework.TestCase;

import org.jfree.chart.JFreeChart;
import static org.mockito.Mockito.*;

import com.atlassian.jira.project.version.Version;

public class VersionHistoryChartFactoryTest extends TestCase {

    static private final SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
    static private final Color RELEASE_COLOR = new Color(224, 224, 224);

    private VersionHistoryChartFactory factory;
    private Version version;
    private VersionWorkloadHistoryManager manager;

    static private Date parse(String date) {
        try {
            return format.parse(date);
        } catch (ParseException e) {
            throw new UnsupportedOperationException("Catch not implemented.");
        }
    }

    static class VersionWorkloadHistoryManagerHelper {

        public static List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate() {
            List<VersionWorkloadHistoryPoint> list = new LinkedList<VersionWorkloadHistoryPoint>();
            list.add(makePoint(parse("06:00 01.01.2005"), 0, 0, 3600, 36000));
            list.add(makePoint(parse("18:00 18.01.2005"), 0, 0, 3600, 36000));
            return list;
        }

        private static VersionWorkloadHistoryPoint makePoint(Date time, long remIssues, long totIssues, long remTime, long totTime) {
            VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
            point.measureTime = time;
            point.remainingIssues = remIssues;
            point.remainingEffort = remTime;
            point.totalIssues = totIssues;
            point.totalEffort = totTime;
            point.type = -1L;
            return point;
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        manager = mock(VersionWorkloadHistoryManager.class);
        when(manager.getWorkloadStartingFromMaxDateBeforeGivenDate(eq(100L), eq(-1L), eq(parse("00:00 01.02.2004"))))
          .thenReturn(VersionWorkloadHistoryManagerHelper.getWorkloadStartingFromMaxDateBeforeGivenDate())
          .thenReturn(VersionWorkloadHistoryManagerHelper.getWorkloadStartingFromMaxDateBeforeGivenDate());
        factory = new VersionHistoryChartFactory(manager);
        version = makeMockVersion();
    }

    private static Version makeMockVersion() {
      Version version = mock(Version.class);
      when(version.getId()).thenReturn(100L);
      when(version.isArchived()).thenReturn(false);
      when(version.isReleased()).thenReturn(false);
      when(version.getReleaseDate()).thenReturn(null);
      when(version.getName()).thenReturn("name");
      return version;
    }

    public void testSimple() {
      Date releaseDate = parse("00:00 01.02.2005");
      when(version.getReleaseDate()).thenReturn(releaseDate);
        JFreeChart chart = factory.makeChart(version, new BurndownPortletConfiguration(parse("00:00 01.02.2004")));
        assertEquals("Chart Title should be picked from the version", "name", chart.getTitle().getText());
        assertEquals("Expected the background color to be normal for non released/archived.", Color.WHITE, chart.getPlot().getBackgroundPaint());
        assertTrue("Release date should be inside the shown range. " + chart.getXYPlot().getDomainAxis().getRange(), releaseDate.getTime() < chart.getXYPlot().getDomainAxis().getRange().getUpperBound());

        assertEquals(Color.BLUE, chart.getXYPlot().getRenderer().getSeriesPaint(1));
        assertEquals("second series should be total time", 10, chart.getXYPlot().getDataset().getYValue(1, 0), 0d);

        assertEquals(Color.DARK_GRAY, chart.getXYPlot().getRenderer().getSeriesPaint(2));
        assertEquals("first series should be remaining time", 1, chart.getXYPlot().getDataset().getYValue(2, 0), 0d);
        
        return;
    }

    public void testCallsHistoryManagerCorrectly() throws Exception {
        when(version.getReleaseDate()).thenReturn(parse("00:00 01.02.2006"));
        JFreeChart chart = new VersionHistoryChartFactory(manager).makeChart(version, new BurndownPortletConfiguration(parse("00:00 01.02.2004")));

        verify(manager).getWorkloadStartingFromMaxDateBeforeGivenDate(eq(100L), eq(-1L), eq(parse("00:00 01.02.2004")));
    }

    public void testWithNullReleaseDate() {
        JFreeChart chart = factory.makeChart(version, new BurndownPortletConfiguration(new Date()));
    }

    public void testArchivedBackground() {
        when(version.isArchived()).thenReturn(true);
        JFreeChart chart = factory.makeChart(version,new BurndownPortletConfiguration(new Date()));
        assertEquals("Archived should have gray background.", RELEASE_COLOR, chart.getPlot().getBackgroundPaint());
        assertTrue("Archived should be included in title", chart.getTitle().getText().indexOf("[archived]") != -1);
    }

    public void testReleasedBackground() {
        when(version.isReleased()).thenReturn(true);
        JFreeChart chart = factory.makeChart(version, new BurndownPortletConfiguration(new Date()));
        assertEquals("Released should have gray background.", RELEASE_COLOR, chart.getPlot().getBackgroundPaint());
        assertTrue("Released should be included in title", chart.getTitle().getText().indexOf("[released]") != -1);
    }
}

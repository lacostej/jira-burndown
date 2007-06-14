/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.project.version.Version;

class VersionHistoryChartFactory {
    
    private static final double SECONDS_PER_HOUR = 3600d;

    private static final DateFormat LONG_TIP = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("fi", "FI"));
    
    private final VersionWorkloadHistoryManager historyManager;

    protected VersionHistoryChartFactory(VersionWorkloadHistoryManager manager) {
        this.historyManager = manager;        
    }   
    
    /**
     * Creates a chart from the version.
     */
    protected JFreeChart makeChart(Version version, Long type, Date startDate) {
        XYSeriesCollection xyDataset = createSeries(version, type, startDate);        
        StandardXYItemRenderer renderer = createRenderer();
        
        DateAxis timeAxis = new DateAxis("");
        timeAxis.setTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());    
        
        
        NumberAxis valueAxis = new NumberAxis(resolveValueAxisName(type));
        valueAxis.setAutoRangeIncludesZero(true);

        XYPlot plot = new XYPlot(xyDataset, timeAxis, valueAxis, renderer);        
        return createChart(version, plot);
    }

    private String resolveValueAxisName(Long type) {
        if (type == -1L) return "Hours of Work";
        return ManagerFactory.getCustomFieldManager().getCustomFieldObject(type).getName();
    }

    private JFreeChart createChart(Version version, XYPlot plot) {
        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(Color.white);
        if (version.isArchived()) {
            chart.getPlot().setBackgroundPaint(new Color(224, 224, 224));
            chart.setTitle(version.getName() + " [archived]");
        } else if (version.isReleased()) {
            chart.getPlot().setBackgroundPaint(new Color(224, 224, 224));
            chart.setTitle(version.getName() + " [released]");
        } else {
            chart.setTitle(version.getName());
        }
        return chart;
    }

    private StandardXYItemRenderer createRenderer() {
        StandardXYToolTipGenerator ttg = new StandardXYToolTipGenerator(
                "{0}: {2}",
                LONG_TIP, NumberFormat.getInstance());
                        
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(
                StandardXYItemRenderer.LINES,
                ttg, new StandardXYURLGenerator("#"));
        renderer.setShapesFilled(true);        
        renderer.setSeriesStroke(0, new BasicStroke(2f));
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(1, new BasicStroke(2f));
        renderer.setSeriesPaint(1, Color.BLUE);        
        renderer.setSeriesShape(2, ShapeUtilities.createUpTriangle(5f));
        renderer.setSeriesPaint(2, Color.GREEN);
        return renderer;
    }

    private XYSeriesCollection createSeries(Version version, Long type, Date startDate) {
        List<VersionWorkloadHistoryPoint> workloadPoints = 
            historyManager.getWorkloadStartingFromMaxDateBeforeGivenDate(version.getId(), type, startDate); 
        setFirstWorkLoadPointToStartDate(startDate, workloadPoints);
        XYSeriesCollection xyDataset = new XYSeriesCollection();
        xyDataset.addSeries(makeSeries("Remaining " + resolveValueAxisName(type), workloadPoints, new RemainingTime()));
        xyDataset.addSeries(makeSeries("Total " + resolveValueAxisName(type), workloadPoints, new TotalTime()));        
        addReleaseMarker(version, xyDataset);
        return xyDataset;
    }

    private void setFirstWorkLoadPointToStartDate(Date startDate, List<VersionWorkloadHistoryPoint> workloadPoints) {
        if (workloadPoints.size() > 0 && workloadPoints.get(0).measureTime.before(startDate)) {
            workloadPoints.get(0).measureTime = startDate;
        }
    }

    private void addReleaseMarker(Version version, XYSeriesCollection xyDataset) {
        if (version.getReleaseDate() != null) {
            XYSeries release = new XYSeries("Release");
            release.add(version.getReleaseDate().getTime(), 0);
            release.add(version.getReleaseDate().getTime(), 10);
            xyDataset.addSeries(release);
        }
    }
    
    private XYSeries makeSeries(String title, List<VersionWorkloadHistoryPoint> workload, Evaluator evaluator) {
        XYSeries series = new XYSeries(title);
        for (VersionWorkloadHistoryPoint point : workload) {
            if (point == null || point.measureTime == null) continue;
            series.add(point.measureTime.getTime(), evaluator.valueOf(point));
        }
        return series;
    }
    
    interface Evaluator {
        public Number valueOf(VersionWorkloadHistoryPoint point);
    }
    
    static class RemainingTime implements Evaluator {        
        public Number valueOf(VersionWorkloadHistoryPoint point) {
            if (point.type == -1L) return point.remainingEffort / SECONDS_PER_HOUR;
            return point.remainingEffort;            
        }
    }
    
    static class TotalTime implements Evaluator {
        public Number valueOf(VersionWorkloadHistoryPoint point) {
            if (point.type == -1L) return point.totalEffort / SECONDS_PER_HOUR;
            return point.totalEffort;
        }
    }
    
}
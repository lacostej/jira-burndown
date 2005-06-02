/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtils;

import com.atlassian.jira.project.version.Version;

class VersionHistoryChartFactory {
    
    private static final double SECONDS_PER_HOUR = 3600d;
    private static final DateFormat LONG_TIP = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, new Locale("fi", "FI"));
    
    private final VersionWorkloadHistoryManager historyManager;

    public VersionHistoryChartFactory(VersionWorkloadHistoryManager manager) {
        this.historyManager = manager;        
    }   
    
    protected JFreeChart makeChart(Version version) {
        XYSeriesCollection xyDataset = new XYSeriesCollection();
        List<VersionWorkloadHistoryPoint> workload = historyManager.getWorkload(version.getId()); 
        xyDataset.addSeries(makeSeries(workload, new RemainingTime()));
        xyDataset.addSeries(makeSeries(workload, new TotalTime()));
        
        addReleaseMarker(version, xyDataset);
        
        StandardXYToolTipGenerator ttg = new StandardXYToolTipGenerator(
                StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                LONG_TIP, NumberFormat.getInstance());
        
        ValueAxis timeAxis = new DateAxis("", SegmentedTimeline.newMondayThroughFridayTimeline());
        
        NumberAxis valueAxis = new NumberAxis("Hours of Work");
        valueAxis.setAutoRangeIncludesZero(true);
                
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(
                StandardXYItemRenderer.LINES,
                ttg, new StandardXYURLGenerator("#"));
        renderer.setShapesFilled(true);
        
        renderer.setSeriesShape(2, ShapeUtils.createUpTriangle(5f));
        renderer.setSeriesStroke(0,new BasicStroke(2f));
        renderer.setSeriesStroke(1,new BasicStroke(2f));
        XYPlot plot = new XYPlot(xyDataset, timeAxis, valueAxis, renderer);
        
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

    private void addReleaseMarker(Version version, XYSeriesCollection xyDataset) {
        if (version.getReleaseDate() != null) {
            XYSeries release = new XYSeries("Release");
            release.add(version.getReleaseDate().getTime(), 0);
            release.add(version.getReleaseDate().getTime(), 10);
            xyDataset.addSeries(release);
        }
    }
    
    private XYSeries makeSeries(List<VersionWorkloadHistoryPoint> workload, Evaluator evaluator) {
        XYSeries series = new XYSeries("Remaining Time");
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
            return point.remainingTime / SECONDS_PER_HOUR;
        }
    }
    
    static class TotalTime implements Evaluator {
        public Number valueOf(VersionWorkloadHistoryPoint point) {
            return point.totalTime / SECONDS_PER_HOUR;
        }
    }
    
}
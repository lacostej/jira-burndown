/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
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
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.project.version.Version;

class VersionHistoryChartFactory {

    private static final DateFormat LONG_TIP = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("fi", "FI"));

    private final VersionWorkloadHistoryManager historyManager;

    protected VersionHistoryChartFactory(VersionWorkloadHistoryManager manager) {
        this.historyManager = manager;
    }

    /**
     * Creates a chart from the version.
     */
    protected JFreeChart makeChart(Version version, BurndownPortletConfiguration config) {
        XYSeriesCollection xyDataset = createSeries(version, config.getTypeId(), config.startDate);
        StandardXYItemRenderer renderer = createRenderer();

        DateAxis timeAxis = new DateAxis("");
        timeAxis.setTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());

        NumberAxis valueAxis = new NumberAxis(resolveValueAxisName(config.getTypeId()));
        valueAxis.setAutoRangeIncludesZero(true);

        XYPlot plot = new XYPlot(xyDataset, timeAxis, valueAxis, renderer);
        JFreeChart createChart = createChart(version, plot);
        if (config.includeLegend) createChart.addLegend(new LegendTitle(plot));
        return createChart;
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
        StandardXYToolTipGenerator ttg = new StandardXYToolTipGenerator("{0}: {2}", LONG_TIP, NumberFormat.getInstance());
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES, ttg, new StandardXYURLGenerator("#"));
        renderer.setShapesFilled(true);
        renderer.setSeriesStroke(0, new BasicStroke(2f));
        renderer.setSeriesShape(0, ShapeUtilities.createUpTriangle(5f));
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesStroke(1, new BasicStroke(2f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(2, new BasicStroke(2f));
        renderer.setSeriesPaint(2, Color.DARK_GRAY);
        renderer.setSeriesStroke(3, new BasicStroke(2f));
        renderer.setSeriesPaint(3, Color.ORANGE);
        return renderer;
    }

    private XYSeriesCollection createSeries(Version releaseVersion, Long estimateType, Date startDate) {
        List<VersionWorkloadHistoryPoint> workloadPoints = historyManager.getWorkloadStartingFromMaxDateBeforeGivenDate(releaseVersion.getId(), estimateType, startDate);
        setFirstWorkLoadPointToStartDate(startDate, workloadPoints);
        XYSeriesCollection xyDataset = new XYSeriesCollection();
        addReleaseMarker(releaseVersion, xyDataset);
        xyDataset.addSeries(makeSeries("Total " + resolveValueAxisName(estimateType), workloadPoints, new TotalTime()));
        if (workloadPoints.size() > 0) {
            xyDataset.addSeries(makeSeries("Scope Adjusted Remaining " + resolveValueAxisName(estimateType), workloadPoints, new CorrectedRemainingTime(workloadPoints.get(workloadPoints.size() - 1))));
            addTrendline(releaseVersion, workloadPoints.get(0).measureTime, xyDataset);
        }
        return xyDataset;
    }

    private void addTrendline(Version releaseVersion, Date startDate, XYSeriesCollection xyDataset) {
        if (releaseVersion.getReleaseDate() != null && xyDataset.getSeries(2).getItemCount() > 2) {
            double[] regression = Regression.getOLSRegression(xyDataset, 2);
            XYSeries series = new XYSeries("Regression");
            addTimeWithRegression(series, regression, startDate.getTime());
            addTimeWithRegression(series, regression, releaseVersion.getReleaseDate().getTime());
            xyDataset.addSeries(series);
        }
    }

    private void addTimeWithRegression(XYSeries series, double[] regression, long timePoint) {
        series.add(timePoint, regression[0] + timePoint * regression[1]);
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
        public Double valueOf(VersionWorkloadHistoryPoint point);
    }

    static class TotalTime implements Evaluator {
        public Double valueOf(VersionWorkloadHistoryPoint point) {
            return point.getCorrectedTotalEffort();
        }
    }

    static class CorrectedRemainingTime implements Evaluator {

        private Double currentTotalEffort;

        public CorrectedRemainingTime(VersionWorkloadHistoryPoint point) {
            currentTotalEffort = point.getCorrectedTotalEffort();
        }

        public Double valueOf(VersionWorkloadHistoryPoint point) {
            return point.getCorrectedRemainingEffort() - (point.getCorrectedTotalEffort() - currentTotalEffort);
        }

    }

}
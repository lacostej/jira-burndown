/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.IOUtil;

/**
 * 
 * @author Jukka Lindstrom
 * @author Markus Hjort
 */
public class ChartPortlet extends PortletImpl {
    File tempDirectory = new File(System.getProperty("java.io.tmpdir"));

    private static final int IMAGE_CREATION_INTERVAL = 60 * 1000;
    private final static Category log = Category.getInstance(ChartPortlet.class);
    private final VersionManager versionManager;
    private final VersionHistoryChartFactory chartService;

    public ChartPortlet(JiraAuthenticationContext authenticationContext, VersionWorkloadHistoryManager manager, VersionManager versionManager, PermissionManager permissionManager, ApplicationProperties properties) {
        super(authenticationContext, permissionManager, properties);
        this.versionManager = versionManager;
        this.chartService = new VersionHistoryChartFactory(manager);
        createTempDir();
    }

    public Map getVelocityParams(PortletConfiguration config) {
        if (config == null) throw new IllegalArgumentException("PortletConfiguration cannot be null.");
        Map model = super.getVelocityParams(config);
        BurndownPortletConfiguration chartConfig = new BurndownPortletConfiguration(config);

        if (chartConfig.getVersionId() == null) return error(model, "Version (" + chartConfig.getVersionId() + ") is not available.");
        if (chartConfig.getVersionId().longValue() < 0L) return error(model, "Please, choose a version. Full Projects are not supported");

        Collection browsableProjects = permissionManager.getProjects(Permissions.BROWSE, authenticationContext.getUser());

        Version version = versionManager.getVersion(chartConfig.getVersionId());
        if (version == null) return error(model, "Version (" + chartConfig.getVersionId() + ") is not available.");
        if (!browsableProjects.contains(version.getProject())) return error(model, "You don't have correct privileges to view this data.");

        try {
            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
            String chartName = chartConfig.createChartName();
            if (createNewImage(new File(tempDirectory, chartName + ".png"))) {
                JFreeChart chart = chartService.makeChart(version, chartConfig);
                saveImageAndTooltipMap(chartName, chartConfig.width, chartConfig.height, chart, info);
            }
            model.put("chartFilename", chartName);
            model.put("imageMapName", chartName);
            model.put("imageMap", readImageMap(chartName));
            model.put("versionId", "" + version.getId());
            model.put("remote", chartConfig.remote);
        } catch (Exception e) {
            log.log(Priority.ERROR, e);
            throw new RuntimeException(e);
        }
        return model;
    }

    private String readImageMap(String chartName) {
        FileReader reader = null;
        try {
            reader = new FileReader(new File(tempDirectory, chartName + ".txt"));
            return IOUtil.toString(reader);
        } catch (Exception e) {
            return null;
        } finally {
            IOUtil.shutdownReader(reader);
        }
    }

    private static Map error(Map model, String string) {
        model.put("errorMessage", string);
        return model;
    }

    /**
     * This is exposed for testing purposes.
     */
    protected boolean createNewImage(File imageFile) {
        return !imageFile.exists() || imageFile.lastModified() < (System.currentTimeMillis() - IMAGE_CREATION_INTERVAL);
    }

    private void saveImageAndTooltipMap(String name, int width, int height, JFreeChart chart, ChartRenderingInfo info) throws IOException {
        ChartUtilities.saveChartAsPNG(new File(tempDirectory, name + ".png"), chart, width, height, info);
        PrintWriter imageMap = new PrintWriter(new FileWriter(new File(tempDirectory, name + ".txt")));
        ChartUtilities.writeImageMap(imageMap, name, info, false);
        IOUtil.shutdownWriter(imageMap);

    }

    private void createTempDir() {
        if (!tempDirectory.exists()) tempDirectory.mkdirs();
    }

}

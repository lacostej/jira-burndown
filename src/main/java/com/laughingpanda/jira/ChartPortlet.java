/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.portal.PortletImpl;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;

public class ChartPortlet extends PortletImpl {

    private final static Category log = Category.getInstance(ChartPortlet.class);
    private final VersionManager versionManager;
    private final VersionHistoryChartFactory chartService;

    public ChartPortlet(
            JiraAuthenticationContext authenticationContext, 
            VersionWorkloadHistoryManager manager, 
            VersionManager versionManager) {
        super(authenticationContext);
        this.versionManager = versionManager;        
        this.chartService = new VersionHistoryChartFactory(manager);
    }

    public String getViewHtml(PortletConfiguration config) { 
        if (config == null)
            throw new IllegalArgumentException("PortletConfiguration cannot be null.");
        int width = 500;
        int height = 300;
        Long versionId = null;
        try {
            width = config.getLongProperty("chart.width").intValue();
            height = config.getLongProperty("chart.height").intValue();
            versionId = config.getLongProperty("versionId");            
        } catch (ObjectConfigurationException e1) {
            log.error(e1);
        }
        if (versionId == null) 
            return "Version (" + versionId + ") is not available.";
        if (versionId.longValue() < 0l)
            return "Please, choose a version. Full Projects are not supported";
        Version version = versionManager.getVersion(versionId);
        if (version == null)
            return "Version (" + versionId + ") is not available.";

        try {
            JFreeChart chart = chartService.makeChart(version);             
            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());  
            String filename = saveImage(versionId, width, height, chart, info);
            return makeHtml(info, filename);
        } catch (Exception e) {
            log.log(Priority.ERROR, e);
            StringWriter content = new StringWriter();
            PrintWriter writer = new PrintWriter(content);
            e.printStackTrace(writer);  
            return content.toString();
        } 
    }

    private String makeHtml(ChartRenderingInfo info, String filename) throws IOException {
        StringWriter out = new StringWriter();
        ChartUtilities.writeImageMap(new PrintWriter(out), filename, info);
        out.write("<img src=\"/jira/servlet/DisplayChart?filename=" + filename + "\" border=0 usemap=\"#" + filename + "\">");
        return out.toString();
    }

    private String saveImage(Long versionId, int width, int height, JFreeChart chart, ChartRenderingInfo info) throws IOException {
        createTempDir();
        File imageFile = new File(new File(System.getProperty("java.io.tmpdir")), "public" + versionId + "-" + width + "x" + height + ".png");
        ChartUtilities.saveChartAsPNG(imageFile, chart, width, height, info);
        return imageFile.getName();
    }

    private void createTempDir() {
        String tempDirName = System.getProperty("java.io.tmpdir");
        if(tempDirName == null)
            throw new RuntimeException("Temporary directory system property (java.io.tmpdir) is null");
        File tempDir = new File(tempDirName);
        if(!tempDir.exists())
            tempDir.mkdirs();
    }
        



    
    
    
    

    

}

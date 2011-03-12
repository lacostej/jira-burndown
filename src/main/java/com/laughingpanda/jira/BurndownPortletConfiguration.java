package com.laughingpanda.jira;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.portal.PortletConfiguration;

class BurndownPortletConfiguration {
    Boolean remote = true;
    Boolean includeLegend = false;
    Boolean includeTrendline = false;
    int width = 500;
    int height = 300;
    private Long versionId = null;
    private Long typeId = -1L;
    Date startDate = null;
    private final PortletConfiguration config;
    
    BurndownPortletConfiguration(Date startDate) {
        config = null;
        this.startDate = startDate;
    }

    public BurndownPortletConfiguration(PortletConfiguration config) {
        this.config = config;
        try {
            width = config.getLongProperty("chart.width").intValue();
            height = config.getLongProperty("chart.height").intValue();
            this.versionId = config.getLongProperty("versionId");

            typeId = getValue(Long.class, "typeId", -1L);
            remote = getValue(Boolean.class, "remote", true);
            includeLegend = getValue(Boolean.class, "chart.includeLegend", false);
            includeTrendline = getValue(Boolean.class, "chart.includeTrendline", false);

            String date = config.getProperty("startDate");
            if (date != null) startDate = getIsoDateFormatter().parse(date);
            else startDate = new Date(0);
        } catch (Exception e) {
            throw new RuntimeException("Error in portlet configuration.", e);
        }
    }

    Long getTypeId() {
        if (typeId == null) return -1L;
        return typeId;
    }

    Long getVersionId() {
        return versionId;
    }

    public Boolean getIncludeLegend() {
        return includeLegend;
    }

    public <T> T getValue(Class<T> type, String propertyName, T defaultValue) {
        try {
            String propertyValue = config.getProperty(propertyName);
            if (propertyValue != null) { return type.getConstructor(String.class).newInstance(propertyValue); }
        } catch (ObjectConfigurationException e) {
            // this is just fine, it means that it cannot be read for now.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return defaultValue;
    }

    public String createChartName() {
        return "public" + getVersionId() + "-" + getTypeId() + "-" + remote + "-" + includeLegend + "-" + includeTrendline + "-"+ getIsoDateFormatter().format(startDate) + "-" + width + "x" + height;
    }

    static SimpleDateFormat getIsoDateFormatter() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

}
package com.laughingpanda.jira;

import java.util.Date;

import com.atlassian.jira.portal.PortletConfiguration;

class BurndownPortletConfiguration {
    Boolean remote = true;
    int width = 500;
    int height = 300;
    Long versionId = null;
    Long typeId = -1L;
    Date startDate = null;

    public BurndownPortletConfiguration(PortletConfiguration config) {
        try {
            width = config.getLongProperty("chart.width").intValue();
            height = config.getLongProperty("chart.height").intValue();
            versionId = config.getLongProperty("versionId");
            String type = config.getProperty("typeId");
            if (type != null) {
                try {
                    typeId = Long.parseLong(type);
                } catch (Exception e) {
                }
            }
            String isRemote = config.getProperty("remote");
            if (isRemote != null) {
                try {
                    remote = Boolean.valueOf(isRemote);
                } catch (Exception e) {
                }
            }
            String date = config.getProperty("startDate");
            if (date != null) startDate = ChartPortlet.getIsoDateFormatter().parse(date);
            else startDate = new Date(0);
        } catch (Exception e) {
            throw new RuntimeException("Error in portlet configuration.", e);
        }
    }

}
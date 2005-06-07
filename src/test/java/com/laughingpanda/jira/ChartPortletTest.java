/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.laughingpanda.mocked.MockFactory;

public class ChartPortletTest extends TestCase {
    ChartPortlet portlet;
    PortletConfiguration config;
    
    static abstract class MockVersion implements Version {
        public MockVersion() {}

        public Long getId() {
            return new Long(1);
        }

        public String getName() {
            return "TestVersion";
        }
        
        public Date getReleaseDate() {
            return new Date(0);
        }
        
        public boolean isArchived() {
            return false;
        }
        
        public boolean isReleased() {
            return false;
        }
        
        
    }
    
    static abstract class MockVersionManager implements VersionWorkloadHistoryManager, VersionManager {
        public MockVersionManager() {}
        
        public Version getVersion(Long id) {
            return  (Version) MockFactory.makeMock(MockVersion.class);
        }

        public List<VersionWorkloadHistoryPoint> getWorkload(Long versionId) {
            return new LinkedList();
        }
        
        
    }        
    
    static abstract class MockConfiguration implements PortletConfiguration {    
        public MockConfiguration() {}

        public Long getLongProperty(String arg0) throws ObjectConfigurationException {
            if ("chart.width".equals(arg0)) return new Long(640);
            if ("chart.height".equals(arg0)) return new Long(400);
            if ("versionId".equals(arg0)) return new Long(1);
            throw new UnsupportedOperationException("Method not implemented." + arg0);
        }
        
        
    }
    
    public void setUp() throws Exception {
        super.setUp();
        config = (PortletConfiguration) MockFactory.makeMock(MockConfiguration.class);
        Object mock = MockFactory.makeMock(MockVersionManager.class);
        portlet = new ChartPortlet(
                null, 
                (VersionWorkloadHistoryManager) mock, 
                (VersionManager) mock);
    }

    
    public void testNullConfiguration() {
        try {
            portlet.getViewHtml(null);
            fail("Expected exception with null configuration.");
        } catch (IllegalArgumentException e) {            
        }        
    }    
    
    public void testBasic() {
        String html = portlet.getViewHtml(config);
        assertTrue("Result: " + html, html.indexOf("<img src=\"/servlet/DisplayChart?filename=public1-640x400.png\" border=0 usemap=\"#public1-640x400.png\">") != -1);
    }
}

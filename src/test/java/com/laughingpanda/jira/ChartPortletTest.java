/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import junit.framework.TestCase;

public class ChartPortletTest extends TestCase {
    ChartPortlet portlet = new ChartPortlet(null, null, null);

    public void testNullConfiguration() {
        try {
            portlet.getViewHtml(null);
            fail("Expected exception with null configuration.");
        } catch (IllegalArgumentException e) {            
        }        
    }
    
    /*
    public void testDimensions() {
        Map map = new HashMap();
        map.put("chart.width", new Long(100));
        map.put("chart.height", new Long(100));
        GenericValue value = new MockGenericValue("portalpage", map);
        PortletConfigurationImpl config = new PortletConfigurationImpl(value);
        portlet.getViewHtml(config);
    }
    */
}

/*
 * Created on 16.5.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.laughingpanda.jira;

import junit.framework.TestCase;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.configurable.XMLObjectConfigurationFactory;

public class VersionWorkloadHistoryServiceTest extends TestCase {

    public void testConfigurationLoadsOk() throws ObjectConfigurationException {
        XMLObjectConfigurationFactory factory = new XMLObjectConfigurationFactory();
        factory.loadObjectConfiguration(VersionWorkloadHistoryService.CONFIG_PATH, VersionWorkloadHistoryService.COMPONENT_ID);
        ObjectConfiguration configuration = factory.getObjectConfiguration(VersionWorkloadHistoryService.COMPONENT_ID, null);
        assertEquals(0,configuration.getFieldKeys().length);
    }
}

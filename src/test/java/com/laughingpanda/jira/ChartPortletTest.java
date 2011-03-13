/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.model.ModelEntity;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.laughingpanda.mocked.MockFactory;
import com.laughingpanda.mocked.NullValues;
import com.opensymphony.user.ProviderAccessor;
import com.opensymphony.user.User;
import com.opensymphony.user.Entity.Accessor;
import com.opensymphony.user.provider.CredentialsProvider;
import static org.mockito.Mockito.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ChartPortletTest {

    ChartPortlet portlet;
    private PortletConfiguration config;

    private MockProviderAccessor accessor;
    private MockJiraAuthenticationContext authenticationContext;
    private User authenticatedUser;
    static abstract class MockVersion implements Version {

        public GenericValue project;

        public MockVersion() {
        }

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

        public GenericValue getProject() {
            return project;
        }
    }

    static abstract class MockProviderAccessor implements ProviderAccessor, CredentialsProvider, PermissionManager {

        public List<GenericValue> accessibleProjects = new LinkedList<GenericValue>();

        public CredentialsProvider getCredentialsProvider(String arg0) {
            return this;
        }

        public boolean load(String arg0, Accessor arg1) {
            return true;
        }

        public Collection getProjects(int permissionType, User user) {
            return accessibleProjects;
        }

        public boolean hasPermission(int permission, User user) {
            return false;
        }

    }

    static abstract class MockVersionManager implements VersionWorkloadHistoryManager, VersionManager {

        public Map<Long, Version> versions = new HashMap<Long, Version>();

        public MockVersionManager() {
        }

        public Version getVersion(Long id) {
            return versions.get(id);
        }

        public List<VersionWorkloadHistoryPoint> getWorkloadStartingFromMaxDateBeforeGivenDate(Long versionId, Long type, Date startDate) {
            return Arrays.asList();
        }
    }

    @Before
    public void setUp() throws Exception {
        NullValues.useDefaultPrimitiveValues();
        authenticationContext = MockFactory.makeMock(MockJiraAuthenticationContext.class);
        accessor = MockFactory.makeMock(MockProviderAccessor.class);
        authenticatedUser = new User("name", accessor);
        authenticationContext.setUser(authenticatedUser);
        config = mock(PortletConfiguration.class);
        MockVersionManager mock = MockFactory.makeMock(MockVersionManager.class);
        portlet = new ChartPortlet(authenticationContext, mock, mock, accessor, NullValues.makeMock(ApplicationProperties.class)) {
            @Override
            protected boolean createNewImage(File imageFile) {
                return true;
            }
        };
        when(config.getProperty("chart.width")).thenReturn("640");
        when(config.getProperty("chart.height")).thenReturn("400");
        when(config.getProperty("versionId")).thenReturn("1");
        when(config.getProperty("startDate")).thenReturn("2005-01-01");

        GenericValue project = new GenericValue(MockFactory.makeMock(ModelEntity.class));
        accessor.accessibleProjects.add(project);

        MockVersion version = MockFactory.makeMock(MockVersion.class);
        version.project = project;

        mock.versions.put(1L, version);
    }

    @Test
    public void testNullConfiguration() {
        try {
            portlet.getVelocityParams(null);
            fail("Expected exception with null configuration.");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Ignore("broken in jira 4.2.2")
    @Test
    public void testBasic() {
        Map params = portlet.getVelocityParams(config);
        assertEquals("public1--1-true-false-false-2005-01-01-640x400", params.get("chartFilename"));
        assertEquals("public1--1-true-false-false-2005-01-01-640x400", params.get("imageMapName"));
        assertEquals("1", params.get("versionId"));
        assertFalse(params.containsKey("errorMessage"));
        assertEquals(true, params.get("loggedin"));
        assertContains((String) params.get("imageMap"), "title");
    }
    
    public static void assertContains(String where, String what) {
        assertTrue(String.format("'%s' didn't contain '%s'.", where, what), where.contains(what));
    }

    @Ignore
    @Test
    public void testUserHasNoRights() {
        accessor.accessibleProjects.clear();
        Map params = portlet.getVelocityParams(config);
        assertFalse(params.containsKey("chartFilename"));
        assertEquals("You don't have correct privileges to view this data.", params.get("errorMessage"));
    }

    @Ignore
    @Test
    public void testNoStartDateConfigured() throws Exception {
        when(config.getProperty("startDate")).thenReturn(null);
        Map params = portlet.getVelocityParams(config);
        assertEquals("public1--1-true-false-false-1970-01-01-640x400", params.get("chartFilename"));
        assertEquals("public1--1-true-false-false-1970-01-01-640x400", params.get("imageMapName"));
        assertFalse(params.containsKey("errorMessage"));
    }

}

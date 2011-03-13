/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindstr�m
 */
package com.laughingpanda.jira;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.portal.PortletConfiguration;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.opensymphony.user.Entity.Accessor;
import com.opensymphony.user.ProviderAccessor;
import com.opensymphony.user.User;
import com.opensymphony.user.provider.CredentialsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.model.ModelEntity;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.swing.*;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ComponentManager.class, UIManager.class})
@SuppressStaticInitializationFor("com.atlassian.jira.ComponentManager")
public class ChartPortletTest {

    ChartPortlet portlet;
    private PortletConfiguration config;

    private ProviderAccessor providerAccessor;
    private CredentialsProvider credentialsProvider;
    private PermissionManager permissionManager;
    private JiraAuthenticationContext authenticationContext;
    private User authenticatedUser;

    @Before
    public void setUp() throws Exception {
        // without this, we get a
        // java.lang.VerifyError: (class: javax/swing/plaf/metal/MetalLookAndFeel, method: getLayoutStyle signature: ()Ljavax/swing/LayoutStyle;) Wrong return type in function
        // at javax.swing.UIManager.getColor(UIManager.java:675)
        // at org.jfree.chart.JFreeChart.<clinit>(JFreeChart.java:239)
        PowerMockito.mockStatic(UIManager.class);

        PowerMockito.mockStatic(ComponentManager.class);
        ComponentManager mockComponentManager = mock(ComponentManager.class);
        when(ComponentManager.getInstance()).thenReturn(mockComponentManager);

        authenticationContext = mock(JiraAuthenticationContext.class);
        providerAccessor = mock(ProviderAccessor.class);
        credentialsProvider = mock(CredentialsProvider.class);
        when(providerAccessor.getCredentialsProvider((String) any())).thenReturn(credentialsProvider);
        permissionManager = mock(PermissionManager.class);
        authenticatedUser = new User("name", providerAccessor);
        when(credentialsProvider.load((String) any(), (Accessor) any())).thenReturn(true);
        when(permissionManager.hasPermission(anyInt(), Matchers.<User>anyObject())).thenReturn(false);
        when(authenticationContext.getUser()).thenReturn(authenticatedUser);

        ModelEntity project = mock(ModelEntity.class);
        when(project.getEntityName()).thenReturn("EntityName");
        GenericValue value = new GenericValue(project);

        Version version = createVersion(value);

        config = mock(PortletConfiguration.class);
        VersionWorkloadHistoryManager manager = mock(VersionWorkloadHistoryManager.class);
        when(manager.getWorkloadStartingFromMaxDateBeforeGivenDate((Long) any(), (Long) any(), (java.util.Date) any())).thenReturn(new java.util.ArrayList());
        VersionManager versions = mock(VersionManager.class);
        when(versions.getVersion(anyLong())).thenReturn(version);
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        portlet = new ChartPortlet(authenticationContext, manager, versions, permissionManager, applicationProperties) {
            @Override
            protected boolean createNewImage(File imageFile) {
                return true;
            }
        };
        when(config.getLongProperty("chart.width")).thenReturn(640L);
        when(config.getLongProperty("chart.height")).thenReturn(400L);
        when(config.getLongProperty("versionId")).thenReturn(1L);
        when(config.getProperty("startDate")).thenReturn("2005-01-01");

        List<GenericValue> projects = new java.util.LinkedList<GenericValue>();
        projects.add(value);
        when(permissionManager.getProjects(anyInt(), (User) any())).thenReturn(projects);

        //applicationProperties.get


    }

    private Version createVersion(GenericValue value) {
        Version version = mock(Version.class);
        when(version.getProject()).thenReturn(value);
        when(version.getName()).thenReturn("TestVersion");
        when(version.getReleaseDate()).thenReturn(new Date(0));
        when(version.isArchived()).thenReturn(false);
        when(version.isReleased()).thenReturn(false);
        when(version.getId()).thenReturn(1L);
        return version;
    }

    @Test
    public void testNullConfiguration() {
        try {
            portlet.getVelocityParams(null);
            fail("Expected exception with null configuration.");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testBasic() {
        Map params = portlet.getVelocityParams(config);
        assertEquals(null, params.get("errorMessage"));
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

    @Test
    public void testUserHasNoRights() {
        List<GenericValue> projects = new java.util.LinkedList<GenericValue>();
        when(permissionManager.getProjects(anyInt(), (User) any())).thenReturn(projects);

        Map params = portlet.getVelocityParams(config);
        assertFalse(params.containsKey("chartFilename"));
        assertEquals("You don't have correct privileges to view this data.", params.get("errorMessage"));
    }

    @Test
    public void testNoStartDateConfigured() throws Exception {
        when(config.getProperty("startDate")).thenReturn(null);
        Map params = portlet.getVelocityParams(config);
        assertEquals("public1--1-true-false-false-1970-01-01-640x400", params.get("chartFilename"));
        assertEquals("public1--1-true-false-false-1970-01-01-640x400", params.get("imageMapName"));
        assertFalse(params.containsKey("errorMessage"));
    }

}

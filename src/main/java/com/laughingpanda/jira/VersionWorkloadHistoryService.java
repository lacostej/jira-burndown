/*
 * $Id$
 * Copyright (c) 2004
 * Jukka Lindström
 */
package com.laughingpanda.jira;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Category;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.service.AbstractService;
import com.opensymphony.module.propertyset.PropertySet;

public class VersionWorkloadHistoryService extends AbstractService {
    
    public static final String CONFIG_PATH = "laughingpanda/services/versionworkloadhistoryservice.xml";
    public static final String COMPONENT_ID = "VERSIONWORKLOADHISTORYSERVICE";
    
    private final class StoreHistoryClosure implements Closure {
        public void execute(Object object) {
            try {
                VersionWorkloadHistoryPoint point = createVersionWorkloadPoint((Version) object);
                versionHistoryManager.storeWorkload(point);
            } catch (GenericEntityException e) {
                throw new RuntimeException(e);
            }
        }

        private VersionWorkloadHistoryPoint createVersionWorkloadPoint(Version version) throws GenericEntityException {
            log.debug("Calculating workload for version : " + version.getName());            
            Collection<GenericValue> issues = versionManager.getFixIssues(version);
            VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
            point.versionId = version.getId();
            point.measureTime = new Date();
            for (GenericValue issue : issues) {
                if (issue == null) continue;
                point.totalIssues++;
                if (issue.getLong("timeoriginalestimate") != null)
                    point.totalTime += issue.getLong("timeoriginalestimate").longValue();

                if (issue.get("resolution") == null) {
                    point.remainingIssues++;
                    if (issue.getLong("timeestimate") != null)
                        point.remainingTime += issue.getLong("timeestimate").longValue();
                }
            }
            return point;
        }
    }
    
    static private final class VersionFilter implements Predicate {
        public boolean evaluate(Object object) {
            Version version = (Version) object;
            return !(version == null || version.isArchived() || version.isReleased());
        }
    }

    static private final class AllFilter implements Predicate {
        public boolean evaluate(Object arg0) {
            return true;
        }
    }

    private final Category log = Category.getInstance(VersionWorkloadHistoryService.class);
    private final VersionWorkloadHistoryManager versionHistoryManager;
    private final SearchRequestManager searchManager;
    private final VersionManager versionManager;
    private final ProjectManager projectManager;

    public VersionWorkloadHistoryService() {
        log.info("Creating.");
        try {
            this.versionManager = ComponentManager.getInstance().getVersionManager();
            if (versionManager == null) throw new RuntimeException("VersionManager cannot be null.");
            this.searchManager = ManagerFactory.getSearchRequestManager();
            if (searchManager == null) throw new RuntimeException("SearchRequestManager cannot be null.");
            this.projectManager = ManagerFactory.getProjectManager();
            if (projectManager == null) throw new RuntimeException("ProjectManager cannot be null.");
            this.versionHistoryManager = (VersionWorkloadHistoryManager) ComponentManager.getInstance().getContainer().getComponentInstanceOfType(VersionWorkloadHistoryManager.class);
            if (versionHistoryManager == null) throw new RuntimeException("VersionWorkloadHistoryManager cannot be null.");
        } catch (RuntimeException e) {
            log.error(e);
            throw e;
        }
        log.info("Created.");
    }

    public void init(PropertySet propertySet) throws ObjectConfigurationException {
        log.info("Initializing.");
    }

    public void run() {
        log.info("Running.");
        try {
            processVersions(new StoreHistoryClosure(), new VersionFilter());
        } catch (Exception e) {
            log.error(e);
        }
        log.info("Finished.");
    }

    /**
     * @throws GenericEntityException
     */
    private void processVersions(Closure closure, Predicate filter) {
        log.info("Processing history.");
        Collection<GenericValue> projects = projectManager.getProjects();
        for (GenericValue project : projects) {
            if (project == null) continue;
            Collection<Version> versions = CollectionUtils.select(versionManager.getVersions(project), filter);
            CollectionUtils.forAllDo(versions, closure);
        }
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException {
        return getObjectConfiguration(COMPONENT_ID, CONFIG_PATH, null);
    }

}

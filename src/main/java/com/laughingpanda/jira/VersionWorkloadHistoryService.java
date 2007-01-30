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
import com.atlassian.jira.issue.DefaultIssueFactory;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
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

        private final IssueTransformer issueTransformer;
        private IssueFactory issueFactory = new DefaultIssueFactory(
                ComponentManager.getInstance().getIssueManager(), 
                ComponentManager.getInstance().getProjectManager(),
                ComponentManager.getInstance().getVersionManager(), 
                ManagerFactory.getIssueSecurityLevelManager(), 
                ComponentManager.getInstance().getConstantsManager(), 
                ComponentManager.getInstance().getSubTaskManager(),
                ComponentManager.getInstance().getFieldManager(),
                ComponentManager.getInstance().getAttachmentManager(),
                ComponentManager.getInstance().getProjectFactory()
        );
                
        public StoreHistoryClosure(IssueTransformer issueTransformer) {
            this.issueTransformer = issueTransformer;
        }

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
            Collection<MutableIssue> issues = issueFactory.getIssues(versionManager.getFixIssues(version));

            VersionWorkloadHistoryPoint total = new VersionWorkloadHistoryPoint();
            total.versionId = version.getId();
            total.measureTime = new Date();
            total.type = issueTransformer.getTypeId();
            for (MutableIssue issue : issues) {
                if (issue == null) continue;
                VersionWorkloadHistoryPoint point = (VersionWorkloadHistoryPoint) issueTransformer.transform(issue);
                if (point == null) continue;                
                total.add(point);
            }
            return total;
        }

    }

    private final Category log = Category.getInstance(VersionWorkloadHistoryService.class);
    private final VersionWorkloadHistoryManager versionHistoryManager;
    private final SearchRequestManager searchManager;
    private final VersionManager versionManager;
    private final ProjectManager projectManager;

    private Closure issueClosure;

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
        super.init(propertySet);
        log.info("Initializing.");
        IssueTransformer issueTransformer = resolveTransformer(propertySet);
        issueClosure = new StoreHistoryClosure(issueTransformer);
    }

    private IssueTransformer resolveTransformer(PropertySet propertySet) throws ObjectConfigurationException {
        if (!hasProperty("service.customFieldId")) return new EstimateTransformer();
        Long id = Long.parseLong(getProperty("service.customFieldId"));
        if (id == null || id < 0) return new EstimateTransformer();
        return new StoryPointTransformer(ManagerFactory.getCustomFieldManager().getCustomFieldObject(id));
    }

    public void run() {
        log.info("Running.");
        try {
            processVersions(issueClosure, new ActiveVersionFilter());
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

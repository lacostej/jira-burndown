package com.laughingpanda.jira;

import com.atlassian.jira.issue.MutableIssue;

final class EstimateTransformer implements IssueTransformer {

    public Object transform(Object object) {
        return this.transformInternal((MutableIssue) object);
    }

    private Object transformInternal(MutableIssue issue) {
        VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
        
        point.type = -1L;
        point.totalIssues++;
        if (issue.getLong("timeoriginalestimate") != null) point.totalEffort += issue.getOriginalEstimate();

        if (issue.getResolution() == null) {
            point.remainingIssues++;
            if (issue.getLong("timeestimate") != null) point.remainingEffort += issue.getEstimate();
        }
        return point;
    }

    public Long getTypeId() {
        return -1L;
    }

}
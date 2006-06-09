
package com.laughingpanda.jira;

import org.apache.commons.collections.Transformer;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;

final class StoryPointTransformer implements IssueTransformer {

    private final CustomField field;

    public StoryPointTransformer(CustomField storyPointField) {
        this.field = storyPointField;
    }

    public Object transform(Object object) {
        return this.transformInternal((MutableIssue) object);
    }

    private Object transformInternal(MutableIssue issue) {
        VersionWorkloadHistoryPoint point = new VersionWorkloadHistoryPoint();
        Double storyPointValue = (Double) issue.getCustomFieldValue(field);
        if (storyPointValue == null) return null;
        
        point.type = getTypeId();
        point.totalIssues = 1;
        point.totalEffort = storyPointValue.longValue();
        if (issue.getResolution() == null) {
            point.remainingIssues++;
            point.remainingEffort = storyPointValue.longValue();
        }
        return point;
    }

    public Long getTypeId() {
        return field.getIdAsLong();
    }

}
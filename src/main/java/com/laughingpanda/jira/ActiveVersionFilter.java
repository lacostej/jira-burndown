
package com.laughingpanda.jira;

import org.apache.commons.collections.Predicate;

import com.atlassian.jira.project.version.Version;

public final class ActiveVersionFilter implements Predicate {
    public boolean evaluate(Object object) {
        if (!(object instanceof Version)) return false;
        Version version = (Version) object;
        return !(version == null || version.isArchived() || version.isReleased());
    }
}
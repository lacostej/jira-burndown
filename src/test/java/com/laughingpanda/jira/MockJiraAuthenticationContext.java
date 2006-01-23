
package com.laughingpanda.jira;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.opensymphony.user.User;

abstract class MockJiraAuthenticationContext implements JiraAuthenticationContext {

    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    
}
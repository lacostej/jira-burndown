package com.laughingpanda.jira;

import org.apache.commons.collections.Transformer;

public interface IssueTransformer extends Transformer {
    
    public Long getTypeId();

}

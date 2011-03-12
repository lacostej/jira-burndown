
package com.laughingpanda.jira;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.configurable.ValuesGenerator;

public class BooleanValuesGenerator implements ValuesGenerator {
    
    Map values = new HashMap();

    public BooleanValuesGenerator() {
        values.put("true", "True");
        values.put("false", "False");
    }

    public Map getValues(Map params) {
        return values;
    }

}

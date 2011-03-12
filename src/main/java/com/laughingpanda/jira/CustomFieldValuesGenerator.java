package com.laughingpanda.jira;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.SequencedHashMap;
import org.apache.log4j.Category;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.issue.fields.CustomField;

public class CustomFieldValuesGenerator implements ValuesGenerator {

    private final Category log = Category.getInstance(CustomFieldValuesGenerator.class);

    public Map getValues(Map params) {
        Map<Long, String> selection = new SequencedHashMap();
        try {
            List<CustomField> customFieldObjects = ManagerFactory.getCustomFieldManager().getCustomFieldObjects();
            selection.put(-1L, "Use builtin 'Estimate' fields" );
            for (CustomField field : customFieldObjects) {
                selection.put(field.getIdAsLong(), field.getName());
            }
        } catch (Exception e) {
            log.error("Exception in resolving possible values.", e);
        }
        return selection;

    }

}

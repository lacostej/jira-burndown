
package com.laughingpanda.jira;

import junit.framework.TestCase;

import com.atlassian.jira.project.version.Version;
import static org.mockito.Mockito.*;

public class ActiveVersionFilterTest extends TestCase {

    ActiveVersionFilter filter = new ActiveVersionFilter();
    
    public void testNullReturnsFalse() throws Exception {
        assertFalse(filter.evaluate(null));
    }
    
    public void testWrongTypeObjectReturnsFalse() throws Exception {
        assertFalse(filter.evaluate(new Object()));
    }
    
    public void testArchivedVersionReturnFalse() throws Exception {
        assertFalse(filter.evaluate(makeMockVersion(true, false)));
    }
    
    public void testReleasedVersionReturnsFalse() throws Exception {
        assertFalse(filter.evaluate(makeMockVersion(false, true)));
    }
    
    public void testActiveVersionReturnTrue() throws Exception {
        assertTrue(filter.evaluate(makeMockVersion(false, false)));
    }

    public static Version makeMockVersion(boolean archived, boolean released) {
      Version version = mock(Version.class);
      when(version.isReleased()).thenReturn(released);
      when(version.isArchived()).thenReturn(archived);
      return version;
    }

}


package com.laughingpanda.jira;

import junit.framework.TestCase;

import com.atlassian.jira.project.version.Version;
import com.laughingpanda.mocked.MockFactory;

public class ActiveVersionFilterTest extends TestCase {

    ActiveVersionFilter filter = new ActiveVersionFilter();
    
    public void testNullReturnsFalse() throws Exception {
        assertFalse(filter.evaluate(null));
    }
    
    public void testWrongTypeObjectReturnsFalse() throws Exception {
        assertFalse(filter.evaluate(new Object()));
    }
    
    public void testArchivedVersionReturnFalse() throws Exception {
        assertFalse(filter.evaluate(MockFactory.makeMock(MockVersion.class, true, false)));
    }
    
    public void testReleasedVersionReturnsFalse() throws Exception {
        assertFalse(filter.evaluate(MockFactory.makeMock(MockVersion.class, false, true)));
    }
    
    public void testActiveVersionReturnTrue() throws Exception {
        assertTrue(filter.evaluate(MockFactory.makeMock(MockVersion.class, false, false)));
    }
    
    
    static abstract class MockVersion implements Version {
        
        private final boolean released;
        private final boolean archived;

        public MockVersion(boolean archived, boolean released) {
            this.archived = archived;
            this.released = released;
        }
        
        public boolean isArchived() {
            return archived;
        }
        
        public boolean isReleased() {
            return released;
        }
    }

}

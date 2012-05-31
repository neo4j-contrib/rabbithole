package org.neo4j.community.console;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 30.05.12
 */
public class UtilTest {
    @Test
    public void testRandomId() throws Exception {
        final String id = Util.randomId();
        assertTrue(id.matches("[0-9a-z]+"));
    }

    @Test
    public void testToId() throws Exception {
        assertEquals("a", Util.toId(10));
        assertEquals("0", Util.toId(0));
        assertEquals("9", Util.toId(9));
        assertEquals("z", Util.toId(35));
    }
}

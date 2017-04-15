package com.kromracing.runningroute.client;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests enum ConnectionType.
 * 
 * This enum allows a string to be converted to and from the enum.
 *
 */
public class ConnectionTypeTest {

    @Test
    public void testStraightLine() {
        ConnectionType type = ConnectionType.fromChar('s');
        assertEquals(ConnectionType.StraightLine, type);
        assertEquals('s', type.toChar());
    }
    
    @Test
    public void testFollowRoad() {
        ConnectionType type = ConnectionType.fromChar('f');
        assertEquals(ConnectionType.FollowRoad, type);
        assertEquals('f', type.toChar());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIllegalArgumentException() {
        ConnectionType.fromChar('X');
    }

}

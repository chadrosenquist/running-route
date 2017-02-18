package com.kromracing.runningroute.client;

/**
 * Used by addVertex() in class RouteLine.  Tells how to connect this point to the previous one.
 *
 */
enum ConnectionType {
    /**
     * Connection to previous vertex is a straight line.
     */
    StraightLine,
    
    /**
     * Connection to previous vertex follows the road.
     */
    FollowRoad,
    ;
    
    /**
     * Converts the connection type to character.
     * @return s if StraightLine, f if FollowRoad.
     */
    public char toChar() {
        switch (this) {
        case StraightLine:
            return 's';
        case FollowRoad:
            return 'f';
        default:
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Converts a character to a connection type.
     * @param value The character to convert.
     * @return StraightLine if value is s, FollowRoad if value is f.
     */
    static public ConnectionType fromChar(final char value) {
        switch (value) {
        case 's':
            return StraightLine;
        case 'f':
            return FollowRoad;
        default:
            throw new IllegalArgumentException("Error: The char " + value + " is invalid.");
        }
    }
}

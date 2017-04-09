package com.kromracing.runningroute.client;

public class InvalidRouteLineException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -6320981702083296098L;
    final String reason;
    
    public InvalidRouteLineException(final String reason) {
        this.reason = reason;
    }
    
    @Override
    public String toString() {
        return reason;
    }
    
    @Override
    public String getMessage() {
        return reason;
    }
}

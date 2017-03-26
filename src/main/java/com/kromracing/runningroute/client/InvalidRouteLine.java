package com.kromracing.runningroute.client;

public class InvalidRouteLine extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -6320981702083296098L;
    final String reason;
    
    public InvalidRouteLine(final String reason) {
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

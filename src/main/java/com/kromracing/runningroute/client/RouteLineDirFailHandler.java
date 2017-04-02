package com.kromracing.runningroute.client;

/**
 * This handler is invoked if Google fails to generate directions.
 *
 */
public interface RouteLineDirFailHandler {
    public void onRouteLineDirectionsFail(int statusCode, String statusCodeName);
}

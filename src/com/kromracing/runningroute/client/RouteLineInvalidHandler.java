package com.kromracing.runningroute.client;

/**
 * This handler is called when RouteLine tries to load a line from a URL
 * and the URL is invalid.
 *
 */
public interface RouteLineInvalidHandler {
    public void onRouteLineInvalid(String reason);
}

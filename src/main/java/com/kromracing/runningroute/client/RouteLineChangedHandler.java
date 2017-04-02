package com.kromracing.runningroute.client;

/**
 * RouteLine calls onRouteChanged() whenever there is a change to the route.
 * This is mostly used to update the distance displayed on the screen.
 *
 */
public interface RouteLineChangedHandler {
    public void onRouteLineChanged();    
}

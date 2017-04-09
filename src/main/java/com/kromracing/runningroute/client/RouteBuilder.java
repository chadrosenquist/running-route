package com.kromracing.runningroute.client;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.kromracing.runningroute.client.ConnectionType;

/**
 * This class is responsible for building and keeping track of the route.
 * It manages the route using the class RouteLine.  With the advent of
 * RouteLine, this class may no longer be necessary.  Consider
 * moving the functionality to RunningRoute.
 * 
 * TO DO: Move the handlers out of RouteLine and into this class.
 * That way, RouteLine can just be the model.
 *
 */
public class RouteBuilder implements RouteLineChangedHandler, RouteLineInvalidHandler,
    RouteLineDirFailHandler {
    private static final double MILES_PER_METER = 0.000621371192;
    
    private final MapWidget map;
    private final Label displayDistance;
    private RouteLine routeLine;    
    
    /**
     * Creates a new Route
     * @param map Google's MapWidget where the route will be mapped.
     * @param displayDistance The calculated distance is displayed here.
     * @param followRoads true = follow the roads, false = straight lines between mouse clicks.
     */
    public RouteBuilder(MapWidget map, Label displayDistance) {
        this.map = map;
        this.displayDistance = displayDistance;
        this.routeLine = new RouteLine(this.map);
        this.routeLine.addRouteLineChangedHandler(this);
        this.routeLine.addRouteLineInvalidHandler(this);
        this.routeLine.addRouteLineDirFailHandler(this);
    }

    /**
     * Handles mouse clicks on the map.
     */
    public void addNewPoint(final LatLng clickPosition, final boolean isFollowRoadChecked) {
        ConnectionType connection;
        if (isFollowRoadChecked)
            connection = ConnectionType.FollowRoad;
        else
            connection = ConnectionType.StraightLine;
        routeLine.addVertex(clickPosition, connection);
    }
  
    /**
     * This method is called whenever the route changed.
     * Added a new point, deleted an existing, moved a marker, ...
     */
    @Override
    public void onRouteLineChanged() {
        calculateDistance();        
    }

    /**
     * Traverses the List route, computing the distance.
     * Distance is computed in meters, and the displayed in miles.
     */
    private void calculateDistance() {
        double distanceInMeters = routeLine.getLengthInMeters();                               
        
        // Update the display.
        // Convert to miles, and then format to two decimal places.
        double distanceInMiles = distanceInMeters * MILES_PER_METER;
        String formattedDistance = NumberFormat.getFormat("0.00").format(distanceInMiles);
        displayDistance.setText(formattedDistance + " miles");    
    }  
    
    /**
     * Undoes the last change.
     */
    public void undo() {
        routeLine.undo();
    }
    
    /**
     * Starts a brand new route (removes all changes).
     */
    public void newRoute() {
        routeLine.newRoute(); 
    }
    
    /**
     * Returns true if there is a route.  False if no route on the map.
     */
    public boolean doesARouteExist() {
        return routeLine.getVertexCount() > 0;
    }

    public void serializeRoute() {
        //final String oldRoute = routeLine.toUrlValue();
        final String oldRoute = "c=42.109319,-87.960856&z=16&s=42.112948,-87.9668&v=s42.115861,-87.956672&v=s42.11379,-87.94739&v=f42.10402,-87.9637&v=s42.105562,-87.954633";        
        routeLine.fromUrlValue(oldRoute);      
    }

    /**
     * RouteLine failed to load from the URL.  Display a message to the user.
     */
    @Override
    public void onRouteLineInvalid(String reason) {
        Window.alert("Failed to load route from URL: " + reason);        
    }

    /**
     * Google couldn't determine directions, so display a message to the user.
     */
    @Override
    public void onRouteLineDirectionsFail(int statusCode, String statusCodeName) {
        Window.alert("An error occurred while mapping route.  " + statusCodeName);        
    }
 }

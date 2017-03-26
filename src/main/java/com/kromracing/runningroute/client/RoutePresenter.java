package com.kromracing.runningroute.client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geocode.Geocoder;
import com.google.gwt.maps.client.geocode.LatLngCallback;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.user.client.History;

/**
 * July 2011
 * 
 * This class is the Presenter.  It communicates between the model (RouteLine) and
 * the view (RouteView)
 *
 */
public class RoutePresenter implements RouteLineChangedHandler, RouteLineInvalidHandler,
    RouteLineDirFailHandler, ValueChangeHandler<String> {
    private static final double MILES_PER_METER = 0.000621371192;
    
    private final MapWidget map;
    private final RouteLine routeLine;   // model    
    private final RouteView routeView;   // view
    
    // Used to find a location.
    private final Geocoder geocoder;   
    
    /**
     * Creates the presenter class
     * @param map Google map to draw route line on
     * @param routeView used to display the GWT widgets
     */
    public RoutePresenter(final MapWidget map, final RouteView routeView) {
        this.map = map;
        this.routeView = routeView;
        this.routeLine = new RouteLine(this.map);
        this.routeLine.addRouteLineChangedHandler(this);
        this.routeLine.addRouteLineInvalidHandler(this);
        this.routeLine.addRouteLineDirFailHandler(this);
        this.geocoder = new Geocoder();
        History.addValueChangeHandler(this);        
        
        // Put an initial distance value.
        calculateDistance();
        
        // If a route exists in the URL, load the route.
        // This is to load a user's bookmarked route.        
        if (!History.getToken().equals(""))
            this.routeLine.fromUrlValue(History.getToken());
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
        updateBrowserUrl();
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
        routeView.setDistance(formattedDistance + " miles");    
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
        if (!doesARouteExist())
            return;
        
        // First, make sure the user really wants to remove all the points.
        if (routeView.confirm("Start a new route?  The current route will be deleted."))                                
            routeLine.newRoute(); 
    }
    
    /**
     * Returns true if there is a route.  False if no route on the map.
     */
    private boolean doesARouteExist() {
        return routeLine.getVertexCount() > 0;
    }

    /*
    public void serializeRoute() {
        final String route = routeLine.toUrlValue();
        routeView.alert(route);   
    }
    */

    /**
     * RouteLine failed to load from the URL.  Display a message to the user.
     */
    @Override
    public void onRouteLineInvalid(String reason) {
        routeView.alert("Failed to load route from URL: " + reason);        
    }

    /**
     * Google couldn't determine directions, so display a message to the user.
     */
    @Override
    public void onRouteLineDirectionsFail(int statusCode, String statusCodeName) {
        routeView.alert("An error occurred while mapping route.  " + statusCodeName + " (" 
                + statusCode + ")");        
    }

    /**
     * This method uses the contents of locationInput to center the map.
     * Uses the Geocoder class.
     * @param location The location to search for.  Ex: 60005  Ex: Barrington, IL
     */
    public void searchForLocation(final String location) {       
        // Find the starting point.
        geocoder.getLatLng(location, new LatLngCallback() {
            @Override
            public void onFailure() {
                routeView.alert("The location '" + location + "' was not found.");                
            }

            @Override
            public void onSuccess(LatLng point) {                
                // Move map to the point.
                map.setCenter(point);
            }           
        });
    }
    
    /**
     * This method is called whenever the history should be updated with the current route.
     * The following cause the history to be changed:
     *   * new Map center
     *   * new zoom
     *   * new point added
     *   * new route
     *   * undo
     */
    public void updateBrowserUrl() {
        History.newItem(routeLine.toUrlValue(), false);
    }

    /**
     * When the URL has changed, load in a new route.
     * @param event
     */
    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
        final String historyToken = event.getValue();
        routeLine.fromUrlValue(historyToken);        
    }
    
    
    
 }

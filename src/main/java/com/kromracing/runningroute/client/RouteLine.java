package com.kromracing.runningroute.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geocode.DirectionQueryOptions;
import com.google.gwt.maps.client.geocode.DirectionQueryOptions.TravelMode;
import com.google.gwt.maps.client.geocode.DirectionResults;
import com.google.gwt.maps.client.geocode.Directions;
import com.google.gwt.maps.client.geocode.DirectionsCallback;
import com.google.gwt.maps.client.geocode.DirectionsPanel;
import com.google.gwt.maps.client.geocode.StatusCodes;
import com.google.gwt.maps.client.geocode.Waypoint;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.Overlay.ConcreteOverlay;
import com.google.gwt.maps.client.overlay.Polyline;
import com.google.gwt.user.client.Timer;


/**
 * Similar to Polyline, but much more advanced.
 * Allows straight lines between points or following the road.
 * Allows the user to edit the lines.
 * 
 * TO DO: This class should only represent the route line as a model.
 * The handlers should be moved to a different class because they
 * modify the model.
 *
 */
public class RouteLine {
    private static final int STARTING_ROUTE_SIZE = 100;
    private static final int URL_PRECISION = 6;
    private static final int DIRECTION_TIMEOUT_MS = 7000;
    private static final int SLOW_REQUEST_MS = 50;
    
    // Map to draw the RouteLine on.    
    private final MapWidget map;
    
    // After the first point, store each vertex and it's connection
    // to the previous point.
    private LatLng firstPoint;
    private final List<LatLng> vertices;
    private final List<ConnectionType> connections;
    
    // Keep a list of all the overlays added to the map
    // so they can be removed or modified.
    // overlaysOnMap[0] = Marker of the start
    // overlaysOnMap[1-N] = Polylines connecting the points.
    private final List<ConcreteOverlay> overlaysOnMap;
    
    // Handlers
    private final Set<RouteLineChangedHandler> routeLineChangedHandlers;
    private final Set<RouteLineInvalidHandler> routeLineInvalidHandlers;
    private final Set<RouteLineDirFailHandler> routeLineDirFailHandlers;
    
    // Queue and Async flag.
    // Asking Google for directions is an asynchronous request.  While the request
    // is going on, queue up requests to add a new vertex.  Drop requests to
    // undo or start a new route.
    private boolean asyncRequest;
    private Timer asyncTimer;
    private Timer slowRequestTimer;   // Used to slow down requests to Google.
    private final Queue<VertexQueueNode> addVertexQueue;
    private static class VertexQueueNode {
        public LatLng vertex;
        public ConnectionType connection;
    }            
      
    /**
     * Constructs a new RouteLine with no points.
     * @param map Map to draw the RouteLine on.
     */
    public RouteLine(final MapWidget map) {
        if (map == null)
            throw new NullPointerException("map is null in RouteLine::RouteLine()");
        this.map = map;
        this.firstPoint = null;
        this.vertices = new ArrayList<LatLng>(STARTING_ROUTE_SIZE);
        this.connections = new ArrayList<ConnectionType>(STARTING_ROUTE_SIZE);
        this.overlaysOnMap = new ArrayList<ConcreteOverlay>(STARTING_ROUTE_SIZE);
        this.routeLineChangedHandlers = new HashSet<RouteLineChangedHandler>();
        this.routeLineInvalidHandlers = new HashSet<RouteLineInvalidHandler>();
        this.routeLineDirFailHandlers = new HashSet<RouteLineDirFailHandler>();
        this.asyncRequest = false;
        this.addVertexQueue = new LinkedList<VertexQueueNode>();
        this.asyncTimer = null;
        this.slowRequestTimer = null;
        
        /* 
        Log.debug("This is a 'DEBUG' test message");
        Log.info("This is a 'INFO' test message");
        Log.warn("This is a 'WARN' test message");
        Log.error("This is a 'ERROR' test message");
        Log.fatal("This is a 'FATAL' test message");
        */
    }    
    
    /**
     * Adds a connection to overlaysOnMap.  Checks the ConnectionType
     * and decides which method to call.
     * @param startIndex Starting index into firstPoint & vertices
     * @param endIndex Ending index into vertices
     * @param connection
     */
    private void addConnectionToMap(final int startIndex, final int endIndex,
            final ConnectionType connection) {
        // Null is valid.  This just means there was only one point on the map.
        if (connection == null)
            return;
        
        switch (connection) {
        case StraightLine:
            addStraightLineToMap(startIndex, endIndex, true);
            break;
        case FollowRoad:
            addFollowRoadToMap(startIndex, endIndex);
            break;
        default:
            throw new IllegalArgumentException("Error: " + connection.toString() + " is unknown in addConnectionToOverlays().");            
        }        
    }

    /**
     * Add a straight line to overlaysOnMap.
     * Implemented using Polyline.
     * @param startIndex Starting index into firstPoint & vertices
     * @param endIndex Ending index into vertices
     * @param signalRouteChanged Set to true if client should be notified that the route has changed.
     */
    private void addStraightLineToMap(final int startIndex, final int endIndex,
            final boolean signalRouteChanged) {
        // Create an array
        final int length = endIndex - startIndex + 1;
        final LatLng points[] = new LatLng[length];
        for (int index = 0; index < length; index++) {
            if ((startIndex + index) == 0)
                points[index] = firstPoint;
            else
                points[index] = vertices.get(startIndex + index - 1);
        }
        
        // Add it to map and overlaysOnMap
        final Polyline polyline = new Polyline(points);
        overlaysOnMap.add(polyline);
        map.addOverlay(polyline);
        
        // Let the client know the route changed.
        if (signalRouteChanged)
            routeChanged();
    }
    
    private void addFollowRoadToMap(final int startIndex, final int endIndex) {
        /* Bug: There is a rate limit enforced by Google.  This limit is unknown.
         * As a simple fix, if the queue length is long, add a small sleep.
         * That way, if the user is clicking, the queue length will be short,
         * and they won't notice the delay.  The delay will only be while
         * loading from a URL.       
         */
        // Set async flag and timeout timer.
        setAsync();
        
        Log.debug("addFollowRoadToMap - Queue size = " + addVertexQueue.size());
        if (addVertexQueue.size() >= 3) {
            if (slowRequestTimer != null)
                slowRequestTimer.cancel();
            slowRequestTimer = new Timer() {
                @Override
                public void run() {
                    slowRequestTimer = null;
                    addFollowRoadToMapInner(startIndex, endIndex);
                    //addFollowRoadToMapInner(startIndex, startIndex + addVertexQueue.size() + 1);
                }                
            };
            slowRequestTimer.schedule(SLOW_REQUEST_MS);            
        }
        else {
            addFollowRoadToMapInner(startIndex, endIndex);
            //addFollowRoadToMapInner(startIndex, startIndex + addVertexQueue.size() + 1);
        }
    }
    
    
    /**
     * Adds a line that follows the road to overlaysOnMap.
     * Implemented using Directions.
     * This function is called by addFollowRoadToMap()
     * @param startIndex Starting index into firstPoint & vertices
     * @param endIndex Ending index into vertices
     */
    private void addFollowRoadToMapInner(final int startIndex, final int endIndex) {        
        // Create an array
        final int length = endIndex - startIndex + 1;
        final Waypoint points[] = new Waypoint[length];
        for (int index = 0; index < length; index++) {
            if ((startIndex + index) == 0)
                points[index] = new Waypoint(firstPoint);
            else
                points[index] = new Waypoint(vertices.get(startIndex + index - 1));
        } 
        
        // Ask Google for Directions.
        MapWidget throwAWayMap = new MapWidget();
        DirectionsPanel throwAWayPanel = new DirectionsPanel();
        DirectionQueryOptions options = new DirectionQueryOptions(throwAWayMap, throwAWayPanel);
        options.setTravelMode(TravelMode.WALKING);
        options.setRetrievePolyline(true);
        options.setRetrieveSteps(true);
        options.setPreserveViewport(true);
        
        Log.debug("LoadFromWaypoints begin.  " + startIndex + " to " + endIndex);
        
        Directions.loadFromWaypoints(points, options, new DirectionsCallback() {       

            @Override
            public void onFailure(int statusCode) {
                Log.error("LoadFromWaypoints FAILED");
                
                clearAsync();
                
                // Remove the vertex and connection.
                vertices.remove(vertices.size()-1);
                connections.remove(connections.size()-1);
                routeLineDirFail(statusCode);
                
                // Continue processes vertices in the queue.
                processAddVertexQueue();
            }

            @Override
            public void onSuccess(DirectionResults result) {   
                Log.debug("LoadFromWaypoints succeeded.");
                
                clearAsync();
                
                // Make a copy of it because result.getPolyline() is on the throw a way map.
                final Polyline throwAWayPolyline = result.getPolyline();
                final int vertexCount = throwAWayPolyline.getVertexCount();
                final LatLng points[] = new LatLng[vertexCount];
                for (int index = 0; index < vertexCount; index++) {
                    points[index] = throwAWayPolyline.getVertex(index);
                }
                
                // Add the Polyline to the real map.
                final Polyline realPolyline = new Polyline(points);
                
                // Update the start and ending points.  This is because the user
                // might click off the road.
                final LatLng start = realPolyline.getVertex(0);
                final LatLng end = realPolyline.getVertex(realPolyline.getVertexCount()-1);
                
                // Modify the starting point.
                if (vertices.size() == 1) {
                    // Starting point is firstPoint
                    firstPoint = start;
                    final Marker oldMarker = (Marker) overlaysOnMap.get(0);
                    map.removeOverlay(oldMarker);
                    final Marker newMarker = new Marker(start);
                    map.addOverlay(newMarker);
                    overlaysOnMap.set(0, newMarker);
                }
                else {
                    // Starting point is in vertices.
                    ConnectionType connection = connections.get(connections.size()-2);
                    
                    switch (connection) {
                    case FollowRoad:
                        // Don't do anything.  Vertex is already in the correct place.
                        break;
                        
                    case StraightLine:
                        // Modify the vertex.
                        vertices.set(vertices.size()-2, start);
                        
                        // Remove the straightline polyline from the map.
                        map.removeOverlay(overlaysOnMap.get(overlaysOnMap.size()-1));
                        overlaysOnMap.remove(overlaysOnMap.size()-1);
                        
                        // Add it back to the map.
                        addStraightLineToMap(vertices.size()-3+1, vertices.size()-2+1, false);
                        break;
                    
                    default:
                        throw new IllegalArgumentException("Error: In onSuccess(), connection = " + connection.toString()
                                + " is not valid.");
                    }
                }
                
                // Update the end point.
                // Because it is the ending point, don't have to worry about
                // modifying an existing overlay.
                vertices.set(vertices.size()-1, end);                  
                
                // Add the real Polyline to the map.
                map.addOverlay(realPolyline);
                overlaysOnMap.add(realPolyline);                   
                
                // Let the client know the route changed.
                routeChanged();
                
                // Continue processes vertices in the queue.
                processAddVertexQueue();
            }
            
        });             
    }
    
    /**
     * Call this method before calling Google to get directions.
     * Sets the async request flag to true and sets the timer.
     */
    private void setAsync() {
        asyncRequest = true;
        if (asyncTimer != null)
            asyncTimer.cancel();
        asyncTimer = new Timer(){
            @Override
            public void run() {
                // Time out!
                asyncRequest = false;
                asyncTimer = null;
                Log.error("Asynchronous call to Google timed out.");
                
                // Continue processes vertices in the queue.
                processAddVertexQueue();
            }        
        };
        asyncTimer.schedule(DIRECTION_TIMEOUT_MS);
    }
    
    /**
     * Clear the async flag and timer.
     */
    private void clearAsync() {
        asyncRequest = false;
        if (asyncTimer != null) {
            asyncTimer.cancel();
            asyncTimer = null;
        }
    }

    /**
     * Adds the first point, as a marker, to map and overlaysOnMap.
     */
    private void addFirstPointToMap() {
        final Marker marker = new Marker(firstPoint);
        overlaysOnMap.add(marker);
        map.addOverlay(marker);
        
        // Let the client know the route changed.
        routeChanged();
    }

   /**
    * Removes the current route from the map and starts a new one.
    * Calls RouteLineChangedHandler.
    */
    public void newRoute() {
        if (asyncRequest)
            return;
        
        // Remove all overlays from the map.
        for (ConcreteOverlay currentOverlay : overlaysOnMap) {
            map.removeOverlay(currentOverlay);
        }
        
        // Remove all overlays from the list.
        overlaysOnMap.clear();
        
        // Clear out the lists.
        firstPoint = null;
        vertices.clear();
        connections.clear();
        
        // Clear out vertex queue.  It should be empty, but clear just in case.
        addVertexQueue.clear();
        
        // Let client know the route has been cleared out.
        routeChanged();
    }
    
    /**
     * Removes the last point from the map.
     * Calls RouteLineChangedHandler.
     */
    public void undo() {
        if (asyncRequest)
            return;
        
        if (firstPoint == null)
            return;
        
        // Remove the overlay.
        map.removeOverlay(overlaysOnMap.get(overlaysOnMap.size()-1));
        overlaysOnMap.remove(overlaysOnMap.size()-1);
        
        if (vertices.size() == 0) {
            firstPoint = null;
        }
        else {        
            vertices.remove(vertices.size()-1);
            connections.remove(connections.size()-1);
        }
        
        // Notify clients.
        routeChanged();
    }
    
    /**
     * Adds a new vertex into the route and adds it onto the map.
     * @param vertex position to add vertex
     * @param connection How to connect this vertex to the previous vertex.
     * Calls RouteLineChangedHandler after new line is successfully drawn on the map.
     */
    public void addVertex(final LatLng vertex, final ConnectionType connection) {
        // Validate input parameters.
        if (vertex == null)
            throw new NullPointerException("vertex in RouteLine::addVertex() is null.");
        if (connection == null)
            throw new NullPointerException("connection in RouteLine::addVertex() is null.");
        
        // Add vertex to the queue.
        final VertexQueueNode node = new VertexQueueNode();
        node.vertex = vertex;
        node.connection = connection;
        addVertexQueue.add(node);
        
        processAddVertexQueue();        
    }
    
    /**
     * Adds all of the vertices in the queue. 
     */
    private void processAddVertexQueue() {      
        // Loop while there is no async request and there are vertices in the queue.
        while ((!asyncRequest) && (!addVertexQueue.isEmpty())) {
            // Retrieve the first element in the queue.
            VertexQueueNode node = addVertexQueue.poll();
            
            // This is the first vertex.
            if (firstPoint == null) {
                firstPoint = node.vertex;  // LatLng is immutable, I think.
                addFirstPointToMap();
            }
            
            // First vertex already exists.
            else {
                vertices.add(node.vertex);
                connections.add(node.connection);
                addConnectionToMap(vertices.size()-1, vertices.size(), node.connection);            
            }
        }        
    }
    
    /**
     * Returns the length of the route in meters.
     * @return Length of route in meters.
     */
    public double getLengthInMeters() {
        double length = 0.0;
        
        for (ConcreteOverlay overlay : overlaysOnMap) {
            if (overlay instanceof Polyline) {
                Polyline polyline = (Polyline) overlay;
                length += polyline.getLength();
            }
        }
        return length;
    }
    
    /**
     * Numbers of vertices in the RouteLine
     * @return vertices
     */
    public int getVertexCount() {
        int count = 0;
        if (firstPoint != null)
            count++;        
        count += vertices.size();
        return count;
    }
    
    /**
     * Sets the handler that is called whenever the route is changed.
     * For example, use the handler to update the distance displayed on the screen.
     * @param routeLineChangedHandler handler that is called when the route changes
     */
    public void addRouteLineChangedHandler(final RouteLineChangedHandler routeLineChangedHandler) {
        routeLineChangedHandlers.add(routeLineChangedHandler);
    }
    
    /**
     * Removes the RouteLineChangedHandler
     * @param routeLineChangedHandler handler to remove
     */
    public void removeRouteLineChangedHandler(final RouteLineChangedHandler routeLineChangedHandler) {
        if (routeLineChangedHandlers.contains(routeLineChangedHandler))
            routeLineChangedHandlers.remove(routeLineChangedHandler);
    }
    
    private void routeChanged() {
        for (RouteLineChangedHandler handler : routeLineChangedHandlers) {
            handler.onRouteLineChanged();
        }
    }
    
    /**
     * Sets the handler that is called when the route in the URL is invalid.
     * @param routeLineInvalidHandler handler that is called when URL is invalid.
     */
    public void addRouteLineInvalidHandler(final RouteLineInvalidHandler routeLineInvalidHandler) {
        routeLineInvalidHandlers.add(routeLineInvalidHandler);
    }
    
    /**
     * Removes the RouteLineInvalidHander
     * @param routeLineInvalidHandler hander to remove
     */
    public void removeRouteLineInvalidHandler(final RouteLineInvalidHandler routeLineInvalidHandler) {
        if (routeLineInvalidHandlers.contains(routeLineInvalidHandler))
            routeLineInvalidHandlers.remove(routeLineInvalidHandler);
    }
    
    private void routeLineInvalid(final String reason) {
        for (RouteLineInvalidHandler handler : routeLineInvalidHandlers) {
            handler.onRouteLineInvalid(reason);
        }
    }
    
    /**
     * Handler is called when Google fails to provide directions.
     * @param routeLineDirFailHandler handler when Google fails to provide directions
     */
    public void addRouteLineDirFailHandler(final RouteLineDirFailHandler routeLineDirFailHandler) {
        routeLineDirFailHandlers.add(routeLineDirFailHandler);
    }
    
    /**
     * Remove the handler when Google fails to provide directions.
     * @param routeLineDirFailHandler handler to remove
     */
    public void removeRouteLineDirFailHandler(final RouteLineDirFailHandler routeLineDirFailHandler) {
        if (routeLineDirFailHandlers.contains(routeLineDirFailHandler))
            routeLineDirFailHandlers.remove(routeLineDirFailHandler);
    }
    
    private void routeLineDirFail(final int statusCode) {
        final String statusCodeName = StatusCodes.getName(statusCode);
        for (RouteLineDirFailHandler handler : routeLineDirFailHandlers) {
            handler.onRouteLineDirectionsFail(statusCode, statusCodeName);
        }
    }

    /**
     * Converts the route into a string that can be used in a URL.
     * @return route as a URL
     */
    public String toUrlValue() {
        final StringBuilder url = new StringBuilder(2500);
        
        // Encode center of map.
        url.append("c=");
        url.append(map.getCenter().toUrlValue(URL_PRECISION));
        
        // Zoom
        url.append("&z=");
        url.append(map.getZoomLevel());
        
        // Append all the vertices.
        if (firstPoint != null) {
            url.append("&s=");
            url.append(firstPoint.toUrlValue(URL_PRECISION));
        }
        for (int index = 0; index < vertices.size(); index++) {
            url.append("&v=");
            url.append(connections.get(index).toChar());
            url.append(vertices.get(index).toUrlValue(URL_PRECISION));
        }
        
        return url.toString();
    }
    
    /**
     * Overrides the current instance with the value read from the string.
     * @param urlValue URL generated by toUrlValue
     */
    public void fromUrlValue(final String urlValue) {
        if (urlValue == null)
            throw new NullPointerException("urlValue is null in RouteLine::fromUrlValue.");
        
        if (asyncRequest)
            return;        
        
        newRoute();
        
        final String[] params = urlValue.split("&");
        
        // Loop through each parameter.
        for (String param : params) {
            // Convert to name/value pair.
            String[] nvPair = param.split("=");
            if (nvPair.length != 2) {
                routeLineInvalid(param + " in the URL is invalid.");
                return;
            }
            String name = nvPair[0];
            String value = nvPair[1];
            
            if (name.equals("c")) {
                map.setCenter(LatLng.fromUrlValue(value));
            }
            else if (name.equals("z")) {
                try {
                    map.setZoomLevel(Integer.parseInt(value));
                }
                catch (final NumberFormatException ex) {
                    routeLineInvalid(name + " in " + param + " is not a valid zoom level.");
                    return;
                }
            }
            else if (name.equals("s")) {
                addVertex(LatLng.fromUrlValue(value), ConnectionType.StraightLine);
            }            
            else if (name.equals("v")) {
                char connectionChar = value.charAt(0);
                if ((connectionChar != 's') && (connectionChar != 'f')) {                    
                    routeLineInvalid(connectionChar + " in " + param + " in the URL is invalid.");
                    return;
                }
                ConnectionType connection = ConnectionType.fromChar(connectionChar);
                String latlng = value.substring(1);
                addVertex(LatLng.fromUrlValue(latlng), connection);
            }         
            else {            
                routeLineInvalid(name + " in " + param + " in the URL is invalid.");
                return;
            }
        } // for
    }
    
    /**
     * How many vertices are left in the add vertex queue.
     * This method is used by RouteLineTest.java
     * @return vertices in the queue
     */
    int addVertexQueueLength() {
        return addVertexQueue.size();
    }
    
    int overlayCount() {
        return overlaysOnMap.size();
    }
}


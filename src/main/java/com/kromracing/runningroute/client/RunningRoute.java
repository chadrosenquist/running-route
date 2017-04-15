package com.kromracing.runningroute.client;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl3D;
import com.google.gwt.maps.client.control.MapTypeControl;
import com.google.gwt.maps.client.control.ScaleControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MapMoveEndHandler;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.maps.client.geocode.Geocoder;
import com.google.gwt.maps.client.geocode.LatLngCallback;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class RunningRoute implements EntryPoint {
    private static final int CONTROL_SIZE_PX = 40;
    private static final int HORIZONTAL_SPACING = 7;
    private static final int LOCATION_VISIBLE_LENGTH = 60;
    private static final String BUTTON_WIDTH = "14ex";
    
    private DockLayoutPanel dock;    
    
    // The Map
    private MapWidget map;
    
    // Location controls.
    private HorizontalPanel locationPanel;
    private Label locationLabel;
    private TextBox locationInput;
    private Button locationSearch;
    
    // Managing the route controls.
    private HorizontalPanel managePanel;
    private Label distanceLabel;
    private Label runningDistance;
    private Button undoButton;
    private Button newRouteButton;
    private CheckBox followRoadCheckBox;
    private Button serializeButton;
    
    // Used to map and manage the route.
    private RouteBuilder routeBuilder;
    
    // Used to find a location.
    private Geocoder geocoder;    
    
    // Handles cookies for RunningRoute
    private RouteCookies cookies;
    

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {                
        dock = new DockLayoutPanel(Unit.PX);
        RootLayoutPanel.get().add(dock);
                
        moduleLoadLocationControls();
        moduleLoadManageControls();                       
        
        // Set focus to the location's input box.
        locationInput.setFocus(true);  
        
        setSearchKeyboardAndMouse();               
        
        /*
         * Asynchronously loads the Maps API.
         *
         * The first parameter should be a valid Maps API Key to deploy this
         * application on a public server, but a blank key will work for an
         * application served from localhost.
         */
        Maps.loadMapsApi("", "2", false, new Runnable() {
            public void run() {
                buildMapsUi();
            }
        });
    }
      
    /**
     * Creates the location controls and adds them to a panel.
     */
    private void moduleLoadLocationControls() {
        // Create location controls.
        locationLabel = new Label("Start Location"); 
        Utils.setId(locationLabel, "label-location");
        locationInput = new TextBox();
        locationInput.setVisibleLength(LOCATION_VISIBLE_LENGTH);
        Utils.setId(locationInput, "textbox-location");
        locationSearch = new Button("Search");
        Utils.setId(locationSearch, "button-search");
        
        // Add location controls to a horizontal panel.
        locationPanel = new HorizontalPanel();
        locationPanel.setSpacing(HORIZONTAL_SPACING);
        locationPanel.add(locationLabel);
        locationPanel.add(locationInput);
        locationPanel.add(locationSearch);  
        
        dock.addNorth(locationPanel, CONTROL_SIZE_PX);  
    }   
    
    /**
     * Creates the controls that distance the current distance
     * and that manage the running route.
     * Adds the controls to a panel.
     */
    private void moduleLoadManageControls() {
        // Create management controls.
        distanceLabel = new Label("Distance:");
        Utils.setId(distanceLabel, "label-distance-literal");
        runningDistance = new Label("");        
        runningDistance.setWidth("130px");
        Utils.setId(runningDistance, "label-distance");
        undoButton = new Button("  Undo   ");
        undoButton.setWidth(BUTTON_WIDTH);
        Utils.setId(undoButton, "button-undo");
        newRouteButton  = new Button("New Route");
        newRouteButton.setWidth(BUTTON_WIDTH);
        Utils.setId(newRouteButton, "button-new-route");
        followRoadCheckBox = new CheckBox("Follow Road");
        Utils.setId(followRoadCheckBox, "checkbox-follow-road");
        serializeButton = new Button("Serialize");
        serializeButton.setWidth(BUTTON_WIDTH);
        
        // Add controls to a horizontal panel.
        managePanel = new HorizontalPanel();
        managePanel.setSpacing(HORIZONTAL_SPACING);
        managePanel.add(distanceLabel);
        managePanel.add(runningDistance);
        managePanel.add(undoButton);
        managePanel.add(newRouteButton);
        managePanel.add(followRoadCheckBox);
        managePanel.add(serializeButton);
        
        dock.addSouth(managePanel, CONTROL_SIZE_PX);
    }
    
    /**
     * Sets up the keyboard and mouse handlers for searching for
     * an address.
     */
    private void setSearchKeyboardAndMouse() {
        // Listen for mouse events on the search button.
        locationSearch.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                searchForLocation();                
            }            
        });
        
        // Listen for enter key on the search text box.
        locationInput.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    searchForLocation();
                }                  
            }            
        });        
    }
    
    /**
     * This method uses the contents of locationInput to center the map.
     * Uses the Geocoder class.
     */
    private void searchForLocation() {
        if (geocoder == null)
            return;
        
        // Find the starting point.
        geocoder.getLatLng(locationInput.getText(), new LatLngCallback() {
            @Override
            public void onFailure() {
                Window.alert("The location '" + locationInput.getText() + "' was not found.");                
            }

            @Override
            public void onSuccess(LatLng point) {                
                // Move map to the point.
                map.setCenter(point);
            }           
        });
    }
    
    /**
     * Setup the Map, event handlers, and Geocoder.
     */
    private void buildMapsUi() {
        cookies = new RouteCookies();
                
        setupMap();
        
        // Now that the map is created, it is okay to create RouteBuilder.
        routeBuilder = new RouteBuilder(map, runningDistance);
        
        // Setup event handlers.
        setupMapEventHandlers();
        setupButtonEventHandlers();                
        
        // Add the map to the dock panel last.
        // The map will take up the remaining space.
        dock.add(map);   
        
        // Geocoder is used to search for a location.
        geocoder = new Geocoder();        
    }

    private void setupMap() {
        // Create map, centering on saved cookie location and zoom level.
        map = new MapWidget(cookies.getMapCenter(), cookies.getMapZoom());       
        map.setSize("100%", "100%");
        Utils.setId(map, "mapwidget-main");
        
        // Add control for zoom level and allow mouse scroll wheel zooming.
        map.addControl(new LargeMapControl3D());   
        map.setScrollWheelZoomEnabled(true);     
    
        // Show the scale
        map.addControl(new ScaleControl());
        
        // Control to change between satellite and map.
        map.addControl(new MapTypeControl());                  
    }

    private void setupMapEventHandlers() {
        // When the user clicks on the map, add a new point.
        map.addMapClickHandler(new MapClickHandler() {
            @Override
            public void onClick(MapClickEvent event) {
                routeBuilder.addNewPoint(event.getLatLng(), followRoadCheckBox.getValue());                
            }            
        });
        
        // Any time the map moves, save the new location.
        map.addMapMoveEndHandler(new MapMoveEndHandler() {
            @Override
            public void onMoveEnd(MapMoveEndEvent event) {
                if (cookies != null)
                    cookies.setMapCenter(map.getCenter());             
            }            
        });
        
        // Any time zoom changes, save the zoom level.
        map.addMapZoomEndHandler(new MapZoomEndHandler() {
            @Override
            public void onZoomEnd(MapZoomEndEvent event) {
                if (cookies != null)
                    cookies.setMapZoom(map.getZoomLevel());                
            }            
        });        
    }
    
    private void setupButtonEventHandlers() {
        // Handler to remove the last point.
        undoButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                routeBuilder.undo();                
            }            
        });
        
        // Handler to remove all of the points.
        newRouteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (!routeBuilder.doesARouteExist())
                    return;
                
                // First, make sure the user really wants to remove all the points.
                if (Window.confirm("Start a new route?  The current route will be deleted."))                
                    routeBuilder.newRoute();                
            }            
        });   
        
        // Serialize (remove later!)
        serializeButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                routeBuilder.serializeRoute();
                
            }
            
        });
    }
}

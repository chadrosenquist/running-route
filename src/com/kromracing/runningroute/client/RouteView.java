package com.kromracing.runningroute.client;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl3D;
import com.google.gwt.maps.client.control.MapTypeControl;
import com.google.gwt.maps.client.control.ScaleControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MapMoveEndHandler;
import com.google.gwt.maps.client.event.MapTypeChangedHandler;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
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
 * July 2011
 * 
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class RouteView implements EntryPoint {
    private static final int CONTROL_SIZE_PX = 40;
    private static final int HORIZONTAL_SPACING = 7;
    private static final int LOCATION_VISIBLE_LENGTH = 60;
    private static final String BUTTON_WIDTH = "14ex";
    private static final String GOOGLE_MAPS_KEY
        = "";
        //= "ABQIAAAAV0P-MmOXteTr3dCPVKRxJxQy67il8HOl4_GYUn_xkFscugdkExSm5LkMJif35E7F2euhrdTMQA-6Kg";    
    
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
    
    // Used to map and manage the route.
    private RoutePresenter routePresenter;    
    
    // Handles cookies for RunningRoute
    private RouteCookies cookies;
    

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {     
        cookies = new RouteCookies();
        
        dock = new DockLayoutPanel(Unit.PX);
        RootLayoutPanel.get().add(dock);
                
        moduleLoadLocationControls();
        moduleLoadManageControls();                       
        
        setSearchKeyboardAndMouse();               
        
        /*
         * Asynchronously loads the Maps API.
         *
         * The first parameter should be a valid Maps API Key to deploy this
         * application on a public server, but a blank key will work for an
         * application served from localhost.
         */
        Maps.loadMapsApi(GOOGLE_MAPS_KEY, "2", false, new Runnable() {
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
        runningDistance.setWidth("120px");
        Utils.setId(runningDistance, "label-distance");
        undoButton = new Button("  Undo   ");
        undoButton.setWidth(BUTTON_WIDTH);
        Utils.setId(undoButton, "button-undo");
        newRouteButton  = new Button("New Route");
        newRouteButton.setWidth(BUTTON_WIDTH);
        Utils.setId(newRouteButton, "button-new-route");
        followRoadCheckBox = new CheckBox("Follow Road");
        Utils.setId(followRoadCheckBox, "checkbox-follow-road");    
        followRoadCheckBox.setValue(cookies.getMapFollowRoad());
        
        // Add controls to a horizontal panel.        
        managePanel = new HorizontalPanel();     
        managePanel.setSpacing(HORIZONTAL_SPACING);
        managePanel.add(distanceLabel);
        managePanel.add(runningDistance);
        managePanel.add(followRoadCheckBox);
        managePanel.add(undoButton);
        managePanel.add(newRouteButton);
        
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
                if (routePresenter != null)
                    routePresenter.searchForLocation(locationInput.getText());                
            }            
        });
        
        // Listen for enter key on the search text box.
        locationInput.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    if (routePresenter != null)
                        routePresenter.searchForLocation(locationInput.getText());
                }                  
            }            
        });        
    }   
    
    /**
     * Setup the Map and event handlers.
     */
    private void buildMapsUi() {                        
        setupMap();
        
        // Now that the map is created, it is okay to create RouteBuilder.
        routePresenter = new RoutePresenter(map, this);
        
        // Setup event handlers.
        setupMapEventHandlers();
        setupButtonEventHandlers();                
        
        // Add the map to the dock panel last.
        // The map will take up the remaining space.
        dock.add(map);            
        
        // Set focus to the location's input box.
        // FireFox requires the focus to be set after Google Maps has loaded up.
        locationInput.setFocus(true);  
    }

    private void setupMap() {
        // Create map, centering on saved cookie location and zoom level.
        map = new MapWidget(cookies.getMapCenter(), cookies.getMapZoom());       
        map.setSize("100%", "100%");
        Utils.setId(map, "mapwidget-main");
        map.setCurrentMapType(cookies.getMapType());
        
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
                final LatLng mapLatLng = event.getLatLng();
                final LatLng overlayLatLng = event.getOverlayLatLng();
                LatLng latLng = null;
                final boolean followRoad = followRoadCheckBox.getValue();
                
                /*
                 * The user can either click on the map or click on an overlay on the map.
                 * If the user clicks on an overlay, event.getLatLng() will return null.
                 * In that case, use event.getOverlayLatLng() as the coordinates instead.
                 */
                if (mapLatLng != null)
                    latLng = mapLatLng;             
                else if (overlayLatLng != null)
                    latLng = overlayLatLng;               
                else
                    throw new IllegalArgumentException("mapLatLng and overlayLatLng are null.  Cannot process the click.");               
                
                routePresenter.addNewPoint(latLng, followRoad);                
            }            
        });
        
        // Any time the map moves, save the new location.
        map.addMapMoveEndHandler(new MapMoveEndHandler() {
            @Override
            public void onMoveEnd(MapMoveEndEvent event) {
                cookies.setMapCenter(map.getCenter()); 
                routePresenter.updateBrowserUrl();
            }            
        });
        
        // Any time zoom changes, save the zoom level.
        map.addMapZoomEndHandler(new MapZoomEndHandler() {
            @Override
            public void onZoomEnd(MapZoomEndEvent event) {
                cookies.setMapZoom(map.getZoomLevel());    
                routePresenter.updateBrowserUrl();
            }            
        });       
        
        // Any time the map type changes, save the map type.
        map.addMapTypeChangedHandler(new MapTypeChangedHandler () {
            @Override
            public void onTypeChanged(MapTypeChangedEvent event) {
                cookies.setMapType(map.getCurrentMapType());
                
            }
        });        
    }
    
    private void setupButtonEventHandlers() {
        // Handler to remove the last point.
        undoButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                routePresenter.undo();                
            }            
        });
        
        // Handler to remove all of the points.
        newRouteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                routePresenter.newRoute();              
            }            
        });   
        
        // Handler to save value of Follow Road in local storage.
        followRoadCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {                
                cookies.setMapFollowRoad(event.getValue());
            }            
        });
    }

    /**
     * Updates the distance label widget.
     * @param distance distance to be displayed.
     */
    public void setDistance(final String distance) {
        runningDistance.setText(distance);
        
    }
    
    /**
     * Displays a popup dialog to the user with only one button.
     * @param message Message the user will see.
     */
    public void alert(final String message) {
        Window.alert(message);
        /*
        final DialogBox dialog = new DialogBox();
        dialog.setText("START dklfajd l desf2304r eslofjaw r320rj elfj320 esflojdsf END");
        final Button close = new Button("Close");
        dialog.setWidget(close);                
        dialog.center();
        dialog.show();
        */
    }
    
    /**
     * Displays a popup dialog asking Yes or No
     * @param message Message the user will see.
     * @return true if yes, false if no
     */
    public boolean confirm(final String message) {
        return Window.confirm(message);
    }    
    
}

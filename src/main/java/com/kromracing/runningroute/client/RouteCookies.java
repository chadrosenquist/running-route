package com.kromracing.runningroute.client;

import java.util.Date;

import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Cookies;

/**
 * Responsible for storing and loading persistent information.
 * If HTML 5 is available, stores information in local storage.  Else,
 * stores in a cookie.
 *
 */
public class RouteCookies {
    private static final long COOKIE_DURATION = 30L * 24L * 3600L * 1000L; // 30 days
    
    private static final String MAP_CENTER = "map_center";  
    private static final double DEFAULT_MAP_CENTER_LAT = 41.878114;        // Chicago
    private static final double DEFAULT_MAP_CENTER_LNG = -87.629798;       // Chicago
    
    private static final String MAP_ZOOM = "map_zoom";
    private static final int DEFAULT_MAP_ZOOM = 13;
    
    private static final String MAP_TYPE = "map_type";
    private static final String DEFAULT_MAP_TYPE = "Map";
    
    private static final String MAP_FOLLOW_ROAD = "map_follow_road";
    private static final String DEFAULT_MAP_FOLLOW_ROAD = "N";             // Y or N  
    
    private final Storage storage;
       
    public RouteCookies() {
        if (Storage.isLocalStorageSupported())
            storage = Storage.getLocalStorageIfSupported();
        else
            storage = null;
    }
    
    /**
     * Saves a key/value pair.
     * Uses local storage if available.  If not, falls back on cookies.
     * @param key key
     * @param value value
     */
    private void setKeyValue(final String key, final String value) {
        if (storage != null) {
            storage.setItem(key, value);
        }
        else {
            final Date currentDate = new Date();
            long expirationDateInMilliSecs = currentDate.getTime() + COOKIE_DURATION;
            final Date expirationDate = new Date(expirationDateInMilliSecs);
            Cookies.setCookie(key, value, expirationDate);
        }
    }
    
    /**
     * Returns a key/value pair.
     * Uses local storage if available.  If not, falls back on cookies.
     * @param key key
     * @return value
     */
    private String getKeyValue(final String key) {
        if (storage != null)
            return storage.getItem(key);       
        else
            return Cookies.getCookie(key);
    }
    
    /**
     * Get the Map's center from a cookie.  If no cookie, use Chicago, IL as the default.
     * @return center of map
     */
    public LatLng getMapCenter() {
        final String mapCenterString = getKeyValue(MAP_CENTER);
        if (mapCenterString == null)
            return LatLng.newInstance(DEFAULT_MAP_CENTER_LAT, DEFAULT_MAP_CENTER_LNG);
        else
            return LatLng.fromUrlValue(mapCenterString);
    }
    /**
     * Stores the map center as a cookie
     * @param mapCenter map center
     */
    public void setMapCenter(LatLng mapCenter) {
        if (mapCenter == null)
            return;
        setKeyValue(MAP_CENTER, mapCenter.toUrlValue());
    }
    
    /**
     * Get the Map's zoom level from a cookie.
     * @return map zoom
     */
    public int getMapZoom() {
        final String mapZoomString = getKeyValue(MAP_ZOOM);
        if (mapZoomString == null)
            return DEFAULT_MAP_ZOOM;
        else
            return Integer.valueOf(mapZoomString);
    }
    /**
     * Stores the map zoom as a cookie
     * @param mapZoom map zoom level
     */
    public void setMapZoom(int mapZoom) {
        setKeyValue(MAP_ZOOM, Integer.toString(mapZoom));
    }
    
    /**
     * Get's the Map's type from a cookie.
     * @return map type
     */
    public MapType getMapType() {
        String mapTypeString = getKeyValue(MAP_TYPE);
        if (mapTypeString == null)
            mapTypeString = DEFAULT_MAP_TYPE;
        if (mapTypeString.equals("Map"))
            return MapType.getNormalMap();
        else if (mapTypeString.equals("Satellite"))
            return MapType.getSatelliteMap();
        else if (mapTypeString.equals("Hybrid"))
            return MapType.getHybridMap();
        else
            return MapType.getNormalMap();

    }

    /**
     * Stores the map type into a cookie
     * @param currentMapType map type
     */
    public void setMapType(final MapType currentMapType) {
        setKeyValue(MAP_TYPE, currentMapType.getName(false));
    }
    
    /**
     * Gets the follow road flag.
     * Y = true, N = false
     * @return
     */
    public boolean getMapFollowRoad() {
        String mapFollowRoadString = getKeyValue(MAP_FOLLOW_ROAD);
        if (mapFollowRoadString == null)
            mapFollowRoadString = DEFAULT_MAP_FOLLOW_ROAD;
        if (mapFollowRoadString.equals("N"))
            return false;
        else if (mapFollowRoadString.equals("Y"))
            return true;
        else {
            // ERROR, unknown value
            return false;
        }
    }
    
    /**
     * Stores the follow road flag.
     * @param follow the value of the Follow road checkbox
     */
    public void setMapFollowRoad(final boolean follow) {
        if (follow)
            setKeyValue(MAP_FOLLOW_ROAD, "Y");
        else
            setKeyValue(MAP_FOLLOW_ROAD, "N");
    }
}

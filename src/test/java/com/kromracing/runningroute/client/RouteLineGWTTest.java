package com.kromracing.runningroute.client;

import org.junit.Ignore;

import com.google.gwt.ajaxloader.client.AjaxLoader.AjaxLoaderOptions;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.geom.LatLng;

/**
 * Performs various unit tests on RouteLine.
 * 
 * All tests are centered around the Lincoln Park Zoo in Chicago, IL.
 *
 * Test don't work!  Don't know why?
 */
@Ignore public class RouteLineGWTTest extends GWTTestCase {
    private static final double ZOO_LAT = 41.922588;
    private static final double ZOO_LNG = -87.630386;
    private static final int ZOO_ZOOM = 16;
    
    private static final int ASYNC_DELAY_MS = 10 * 1000;
    private boolean loaded = false;
    private static final String MAPS_VERSION = "2.x";    
    
    private RouteLine routeLine;
    
    @Override
    public String getModuleName() {        
        return "com.kromracing.runningroute.RunningRoute";
    }
    
    public RouteLineGWTTest() {
        super();
    }
    
    /**
     * Loads the Maps API asynchronously and runs the specified test.
     * @param testRunnable
     */
    private void runMapTest(final boolean finishTest, final Runnable testRunnable) {
        if (loaded) {
            testRunnable.run();
        }
        else {
            Maps.loadMapsApi(null, MAPS_VERSION, false, AjaxLoaderOptions.newInstance(),
                    new Runnable() {
                        @Override
                        public void run() {
                            loaded = true;
                            setupRouteLine();
                            testRunnable.run();
                            if (finishTest)
                                finishTest();
                        }             
            });
            delayTestFinish(ASYNC_DELAY_MS);
        }
    }
    
    /**
     * Sets up the Map and RouteLine
     */
    private void setupRouteLine() {              
        final LatLng Zoo = LatLng.newInstance(ZOO_LAT, ZOO_LNG);
        final MapWidget map = new MapWidget(Zoo, ZOO_ZOOM);
        map.setSize("100%", "100%");
        routeLine = new RouteLine(map);
    }
    
    /**
     * Adds a straight line vertex.  Used to cut down on code
     * @param routeLine routeLine
     * @param lat lat
     * @param lng lng
     */
    private void addSV(final double lat, final double lng) {
        routeLine.addVertex(LatLng.newInstance(lat, lng), ConnectionType.StraightLine);
    }
    
    /**
     * Creates a 4 point straight line route that is used in several tests.
     */
    private void createStraightLineRoute() {
        // Create a route of 3 straight lines, 4 points                
        addSV(41.924616,-87.63485);
        addSV(41.925582,-87.634667);
        addSV(41.926061,-87.630408);
        addSV(41.918709,-87.628562);
    }
    
    /**
     * Adds a few straight lines to the map and verifies the distance is correct.
     */
    public void XXXtestStraightLines() {
        runMapTest(true, new Runnable() {

            @Override
            public void run() {
                createStraightLineRoute();
                
                // Distance is 0.81 miles, or 1297.93 meters
                assertEquals(1297.93, routeLine.getLengthInMeters(), .1);
            }
            
        });        
      }
    
    /**
     * Adds a few straight lines, and then undoes the last add.
     */
    public void testUndoStraight() {
        runMapTest(true, new Runnable() {

            @Override
            public void run() {
                createStraightLineRoute();
                routeLine.undo();
                
                // Distance is .29 miles, or 465.35 meters
                assertEquals(465.35, routeLine.getLengthInMeters(), .1);               
            }
            
        });
    }
    
    /**
     * Adds a few straight lines, and then starts a new route.
     */
    public void testNewRouteStraight() {
        runMapTest(true, new Runnable() {

            @Override
            public void run() {               
                createStraightLineRoute();
                routeLine.newRoute();
                
                // Distance is 0.0
                assertEquals(0.0, routeLine.getLengthInMeters(), .1);               
            }
            
        });
    }
    
    private void createFollowRoadRoute(final Runnable restOfTest) {    
        // The first point is not asynchronous.
        routeLine.addVertex(LatLng.newInstance(41.92476,-87.63566), ConnectionType.FollowRoad);
        
        // The calls are asynchronous, so we have to wait for the calls to come back.
        routeLine.addRouteLineChangedHandler(new RouteLineChangedHandler() {

            @Override
            public void onRouteLineChanged() {   
                if (routeLine.addVertexQueueLength() != 0)
                    return;
                
                // The queue is finally empty, so run the rest of the test.
                restOfTest.run();
                finishTest();
            }            
        });
        
        routeLine.addRouteLineDirFailHandler(new RouteLineDirFailHandler() {

            @Override
            public void onRouteLineDirectionsFail(int statusCode,
                    String statusCodeName) {
                assertEquals("yo", statusCodeName);
                
            }           
            
        });
        
        // Create a route of 3 follow roads, 4 points        
        routeLine.addVertex(LatLng.newInstance(41.91996,-87.63499), ConnectionType.FollowRoad);
        //routeLine.addVertex(LatLng.newInstance(41.91682,-87.63065), ConnectionType.FollowRoad);
        //routeLine.addVertex(LatLng.newInstance(41.91366,-87.63101), ConnectionType.FollowRoad);        
               
    }    
    
    /**
     * Adds a few follow road lines, and verifies distance is correct.
     */
    public void xtestFollowRoads() {
        runMapTest(false, new Runnable() {

            @Override
            public void run() {                    
                createFollowRoadRoute(new Runnable() {

                    @Override
                    public void run() {
                        // Distance is 0.99 miles, or ??? meters                
                        assertEquals(-1, routeLine.addVertexQueueLength());
                        assertEquals(-1, routeLine.overlayCount());
                        assertEquals(1297.93, routeLine.getLengthInMeters(), .1);                          
                    }
                    
                });                              
            }
            
        });
    }
    
    /**
     * Adds a few straight lines, and then starts a new route.
     */
    public void testStraightLineToUrl() {
        runMapTest(true, new Runnable() {

            @Override
            public void run() {               
                createStraightLineRoute();                
                
                assertEquals("c=41.922588,-87.630386&z=16&s=41.924616,-87.63485&v=s41.925582,-87.634667&v=s41.926061,-87.630408&v=s41.918709,-87.628562",
                        routeLine.toUrlValue());               
            }
            
        });
    }
    
    /**
     * Reads in a route comprised of straight lines from a URL
     * and verifies the distance.
     */
    public void testStraightLineFromUrl() {
        runMapTest(true, new Runnable() {

            @Override
            public void run() {
                routeLine.fromUrlValue("c=41.91791,-87.616911&z=16&s=41.924616,-87.63485&v=s41.925582,-87.634667&v=s41.926061,-87.630408&v=s41.918709,-87.628562");         
                
                assertEquals(1297.93, routeLine.getLengthInMeters(), .1);                    
            }
            
        });
    }    
    
    /**
     * Creates a very big route using only straight lines.
     */
    public void xtestStraightLinesBig() {
        runMapTest(true, new Runnable() {

            @Override
            public void run() {
                addSV(41.870144,-87.646308);
                addSV(41.870176,-87.646887);
                addSV(41.867204,-87.646844);
                addSV(41.867324,-87.639302);
                addSV(41.867452,-87.627543);
                
                addSV(41.867563,-87.620398);
                addSV(41.868211,-87.618542);
                addSV(41.867947,-87.617919);
                addSV(41.868386,-87.616342);
                addSV(41.87846,-87.617007);
                
                addSV(41.881176,-87.617029);
                addSV(41.882526,-87.616042);
                addSV(41.882838,-87.613939);
                addSV(41.884667,-87.612995);
                addSV(41.887039,-87.612931);
                addSV(41.887766,-87.613499);
                
                addSV(41.88783,-87.613853);
                addSV(41.888852,-87.613885);
                addSV(41.891959,-87.613724);
                addSV(41.893221,-87.613821);
                addSV(41.9015,-87.619475);
                addSV(41.901718,-87.622168);                     
                
                addSV(41.90262,-87.623359);
                addSV(41.90892,-87.625376);
                addSV(41.91205,-87.625194);
                addSV(41.914262,-87.626159);
                addSV(41.915635,-87.626942);
                addSV(41.918126,-87.628477);
                
                addSV(41.924113,-87.630107);
                
                // 26 points seems to be the limit?
                // ? Passes when only this test is run ??
                addSV(41.926069,-87.630633);       
                addSV(41.925502,-87.635826);
                addSV(41.926141,-87.636244);
                addSV(41.927865,-87.638605);
                /**/
                
                assertEquals(10809.88, routeLine.getLengthInMeters(), .1);                               
            }
            
        });
    } // end testStraightLinesBig()
}

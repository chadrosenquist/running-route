# running-route
Map out your running route using Google Maps and Google Web Toolkit (GWT).

# Compile

```
mvn clean
mvn compile
mvn test
mvn gwt:compile
```

# Output
Open the following HTML file:

`target/running-route-${version}/RunningRoute.html`

For example:

`target/running-route-1.4-SNAPSHOT/RunningRoute.html`


`file:///D:/Git/running-route/target/running-route-1.4-SNAPSHOT/RunningRoute.html`

# Revision History
### 1.3
- Works with IE 11 and Windows 7.
- Ported to new version of GWT and Selenium.
- When loading from a URL, Google times out after 12 "clicks".  The workaround fix is RouteLine::setAsync::run() calls processAddVertexQueue() to keep processing.  Unfortunately, the line that times out is never drawn.

### 1.2
- Fixed bug.  If the user loads a long route from a bookmark that has all follow roads, Google will come back with the error: TOO_MANY_QUERIES (630).  This is because Google has a limit to how many queries per second it will accept.  This limit is unknown. The fix is if the add vertex queue size is >= 3, sleep for 50ms between requests to Google.
- Refactored enum ConnectionType into it's own file, reducing the size of RouteLine.java
- The value of the Follow Road check box is saved in local storage/cookies.

### 1.1
- Fixed bug.  If the user clicks on route line, the web site starts working.  The root cause is in MapClickHandler::onClick().  If the user clicks on an overlay, event.getLatLng() returns null.  Use event.getOverlayLatLng() in that case.
- Visual theme changed to Dark.

### 1.0
Initial release.

# Design
The files are located at:

`src/main/java/com/kromracing/runningroute/client`

* RunningRoute.java - Main entry point to this website.
* RouteView.java (View) - Contains the user interface.
* RouteLine.java (Model) - Similar to Polyline, but much more advanced.  Allows straight lines between points or following the road.  Allows the user to edit the lines.
    * ConnectionType.java - Used by addVertex() in class RouteLine.  Tells how to connect this point to the previous one.
* RoutePresenter.java (Presenter) - It communicates between the model (RouteLine) and the view (RouteView).
* RouteCookies.java - Responsible for storing and loading persistent information. If HTML 5 is available, stores information in local storage.  Else, stores in a cookie.
* Handlers
    * RouteLineChangedHandler - RouteLine calls onRouteChanged() whenever there is a change to the route.  This is mostly used to update the distance displayed on the screen.
    * RouteLineDirFailHandler - This handler is invoked if Google fails to generate directions.
    * RouteLineInvalidHandler - This handler is called when RouteLine tries to load a line from a URL and the URL is invalid.
* RouteBuild.java - This class is responsible for building and keeping track of the route.  It manages the route using the class RouteLine.
* Utils.java - Provides various utilities.

# Tests
TO DO
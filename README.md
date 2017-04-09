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

# Design
The files are located at:

`src/main/java/com/kromracing/runningroute/client`

* RunningRoute.java - Main entry point to this website.
* RouteLine.java - Similar to Polyline, but much more advanced.  Allows straight lines between points or following the road.  Allows the user to edit the lines.
* ConnectionType.java - Used by addVertex() in class RouteLine.  Tells how to connect this point to the previous one.
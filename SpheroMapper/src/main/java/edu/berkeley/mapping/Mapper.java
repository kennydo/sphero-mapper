package edu.berkeley.mapping;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;
import com.vividsolutions.jts.triangulate.quadedge.Vertex;
import com.vividsolutions.jtstest.function.CreateRandomShapeFunctions;
import com.vividsolutions.jtstest.function.CreateShapeFunctions;
import com.vividsolutions.jtstest.testrunner.GeometryOperationLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

/**
 * The class that implements the mapping algorithm.
 * @version 1.0
 */
public class Mapper {
	/**
	 * Enumeration that defines all possible states of mapping algorithm.
	 */
	public static enum State{
		/**
		 * The state when the algorithm hasn't begun yet.
		 */
		IDLE,
		/**
		 * The state of the mapping algorithm in which the robot must search for obstacles.
		 */
		SEARCH_OBSTACLE,
		/**
		 * The state of the mapping algorithm in which the robot must try to contour the obstacle that was found.
		 */
		CONTOUR_OBSTACLE,
		/**
		 * The state when the mapping is following a path to get to an undiscovered area.
		 */
		FOLLOW_PATH,
		/**
		 * The state when the mapping is finished.
		 */
		FINISHED
	};
	
	public static String stateName(State state) {
		String name = new String();
		switch (state) {
			case IDLE:
				name = "IDLE";
				break;
			case SEARCH_OBSTACLE:
				name = "SEARCH_OBSTACLE";
				break;
			case CONTOUR_OBSTACLE:
				name = "CONTOUR_OBSTACLE";
				break;
			case FOLLOW_PATH:
				name = "FOLLOW_PATH";
				break;
			case FINISHED:
				name = "FINISHED";
				break;
		}
		return name;
	}

	/**
	 * The commander object for the mapper.
	 */
	private Commander commander;
	
	/**
	 * The algorithm parameters.
	 */
	private Parameters parameters = new Parameters();
	
	/**
	 * The current state of the algorithm.
	 */
	private State state = State.IDLE;
	
	/**
	 * List of listeners to be triggered every time a state is set.
	 */
	private final ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<StateChangeListener>();
	
	/**
	 * A random number generator to be used temporarily in <code>calculateDriveHeadingVariation</code> method.
	 * @see #calculateDriveHeadingVariation() 
	 */
	private final Random random = new Random();
	
	/**
	 * The geometry factory for the mapping process.
	 */
	private final GeometryFactory geometryFactory = new GeometryFactory();
	
	/**
	 * A random points builder
	 */
	private RandomPointsBuilder randomPointsBuilder = new RandomPointsBuilder(geometryFactory);
	
	/**
	 * The geometry object to represent the objects on the ground.
	 */
	private Geometry objectsGeometry = geometryFactory.createMultiPolygon(null);
	
	/**
	 * The geometry object to represent the free ground, that is, the areas where there's nothing on.
	 */
	private Geometry freeGeometry = geometryFactory.createMultiPolygon(null);
	
	private Geometry perimeterGeometry = geometryFactory.createMultiPolygon(null);
	
	private Geometry pathGeometry = geometryFactory.createGeometry(null);
	
	private Geometry edgesGeometry = geometryFactory.createGeometry(null);
	
	/**
	 * Last event reported.
	 */
	private MappingEvent lastEvent;
	
	/**
	 * The start point of the contour process
	 */
	private final Coordinate contourStartPoint = new Coordinate();
	
	private final ArrayList<Coordinate> obstaclePoints = new ArrayList<Coordinate>();
	
	/**
	 * The heading variation to be set if the state needs to use the drive command.
	 * @see #setState(edu.berkeley.mapping.Mapper.State, edu.berkeley.mapping.MappingEvent) 
	 */
	private float headingVariation;
	

	private ArrayList<Coordinate> pathPoints = new ArrayList<Coordinate>();
	
	/**
	 * 
	 * @param commander The commander object for the mapper.
	 */
	public Mapper(Commander commander) {
		this.commander = commander;
	}
	
	/**
	 * 
	 * @param commander The commander object for the mapper.
	 * @param parameters The parameters for the algorithm.
	 */
	public Mapper(Commander commander, Parameters parameters){
		this(commander);
		this.parameters = parameters;
	}
	
	private void addFreeGeometry(Geometry freeGeometry){
		this.freeGeometry = this.freeGeometry.union(freeGeometry);
	}
	
	private void addObjectGeometry(Geometry obejctGeometry){
		objectsGeometry = objectsGeometry.union(obejctGeometry);
	}
	
	/**
	 * This method must be called by the "robot side" implementation to report
	 * any relevant event to the mapper.
	 * @param event The event reported.
	 */
	public void reportEvent(MappingEvent event){
		System.out.println("State = " + stateName(state));
		switch(state){
			case IDLE:
				switch(event.getType()){
					case START:
						headingVariation = 0;
						setState(State.SEARCH_OBSTACLE, event);
						break;
					default:
						mappingEventNotExpected(event);
				}
				break;
			case SEARCH_OBSTACLE:
				switch(event.getType()){
					case DISTANCE_REACHED:
					{
						Geometry g = generateDistanceReachedGeometry(event);
						addFreeGeometry(g);
						headingVariation = 0;
						setState(isDone() ? State.FINISHED : State.SEARCH_OBSTACLE, event);
						break;
					}
					case COLLISION:
						Geometry g = generateDistanceReachedGeometry(event);
						freeGeometry = freeGeometry.union(g);
						if (pointNotVisited(event)) {
							System.out.println("Point NOT visited.");
							contourStartPoint.setOrdinate(Coordinate.X, event.getX());
							contourStartPoint.setOrdinate(Coordinate.Y, event.getY());
							obstaclePoints.clear();
							/*obstaclePoints.add(new Coordinate(
									event.getX() + Math.cos(event.getAngle()*Math.PI/180f),
									event.getY() + Math.sin(event.getAngle()*Math.PI/180f)
							));*/
							obstaclePoints.add(new Coordinate(event.getX(), event.getY()));
							setState(State.CONTOUR_OBSTACLE, event);
						} else {
							System.out.println("Point VISITED.");
							if (perimeterGeometry.isEmpty()) { //I think the perimeterGeometry,isEmpty() always return at this point... ???
								headingVariation = calculateDriveHeadingVariation(event);
								setState(State.SEARCH_OBSTACLE, event);
							} else { //I think this is unreachable
								pathPoints = calculatePathPoints(event);
								if (pathPoints == null) {
									setState(State.FINISHED, event);
								} else {
									setState(State.FOLLOW_PATH, event);
								}
							}
						}
						break;
					case POINT_REACHED:
						isDone();
						break;
					case POINTS_COMPLETED: //temporary
						setState(State.SEARCH_OBSTACLE, event);
						isDone();
					default:
						mappingEventNotExpected(event);
				}
				break;
			case CONTOUR_OBSTACLE:
				switch(event.getType()){
					case SQUARE_COMPLETED:
						break;
					case COLLISION:
						Geometry g = generateDistanceReachedGeometry(event);
						freeGeometry = freeGeometry.union(g);
						Coordinate possibleEndPoint = new Coordinate(event.getX(), event.getY());
						obstaclePoints.add(new Coordinate(event.getX(), event.getY()));
						if(possibleEndPoint.distance(contourStartPoint) <= parameters.getContourFinishPointRadiusThreshold()){
							obstaclePoints.add(obstaclePoints.get(0));
							Polygon obstacle = geometryFactory.createPolygon(obstaclePoints.toArray(new Coordinate[obstaclePoints.size()]));
							Point p = geometryFactory.createPoint(possibleEndPoint);
							
							//
							
							BufferParameters bufferParameters = new BufferParameters();
							//bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
							BufferOp bufferOp = new BufferOp(obstacle, bufferParameters);
							Geometry buf = bufferOp.getResultGeometry(parameters.getRunnerWidth() * 2);
							
							if(freeGeometry.within(buf)){
								System.out.println("Perimeter found.");
								Geometry buf2 = bufferOp.getResultGeometry(parameters.getRunnerWidth());
								perimeterGeometry = perimeterGeometry.union(buf2);
							}else{
								addObjectGeometry(obstacle);
							}
							
							System.out.println(perimeterGeometry.getNumGeometries());
							System.out.println(perimeterGeometry.toString());
							if (perimeterGeometry.isEmpty()) {
								System.out.println("Perimeter is empty.");
								headingVariation = calculateDriveHeadingVariation(event);
								setState(State.SEARCH_OBSTACLE, event);
							} else {
								System.out.println("Perimeter is not empty.");								
								pathPoints = calculatePathPoints(event);
								if (pathPoints == null) {
									setState(State.FINISHED, event);
								} else {
									setState(State.FOLLOW_PATH, event);
								}
							}
						}else{
							setState(State.CONTOUR_OBSTACLE, event);
						}
						break;
					default:
						mappingEventNotExpected(event);
				}
				break;
			case FOLLOW_PATH:
				switch(event.getType()){
					case COLLISION:
						Geometry g = generateDistanceReachedGeometry(event);
						freeGeometry = freeGeometry.union(g);
						if (pointNotVisited(event)) {
							System.out.println("In FOLLOW_PATH, COLLISION, point NOT visited.");
							contourStartPoint.setOrdinate(Coordinate.X, event.getX());
							contourStartPoint.setOrdinate(Coordinate.Y, event.getY());
							obstaclePoints.clear();
							obstaclePoints.add(new Coordinate(event.getX(), event.getY()));
							setState(State.CONTOUR_OBSTACLE, event);
						} else {
							System.out.println("In FOLLOW_PATH, COLLISION, point visited.");
							if (perimeterGeometry.isEmpty()) {//I think perimeterGeometry.isEmpty() alwas returns false at this point.
								headingVariation = calculateDriveHeadingVariation(event);//I think this is unreachable.
								setState(State.SEARCH_OBSTACLE, event);
							} else {
								pathPoints = calculatePathPoints(event);
								if (pathPoints == null) {
									setState(State.FINISHED, event);
								} else {
									setState(State.FOLLOW_PATH, event);
								}
							}
						}
						break;
					case POINT_REACHED:
						System.out.println("In FOLLOW_PATH, POINT_REACHED.");
						g = generateDistanceReachedGeometry(event);
						freeGeometry = freeGeometry.union(g);
						break;
					case POINTS_COMPLETED:
						System.out.println("In FOLLOW_PATH, POINTS_COMPLETED.");
						g = generateDistanceReachedGeometry(event);
						freeGeometry = freeGeometry.union(g);
						if (perimeterGeometry.isEmpty()) {//I think perimeterGeometry.isEmpty() alwas returns false at this point.
							//I think this is unreachable.
							headingVariation = calculateDriveHeadingVariation(event);
							setState(State.SEARCH_OBSTACLE, event);
						} else {
							pathPoints = calculatePathPoints(event);
							setState(State.FOLLOW_PATH, event);
						}
						break;
					default:
						mappingEventNotExpected(event);
						break;
				}
				break;
		}
	}
	
	/**
	 * Defines the next state and take the correspondent actions.
	 * @param state The new state to be set.
	 * @param event The event for this transition.
	 */
	private void setState(State state, MappingEvent event){
		lastEvent = event;
		State oldState = this.state;
		this.state = state;
		switch(state){
			case FINISHED:
				commander.stop();
				break;
			case SEARCH_OBSTACLE:
				commander.drive(headingVariation, parameters.getCommanderDriveDistance());
				if(!perimeterGeometry.isEmpty()){ //Temporary for tests
					//I think this block isn't necessary anymore.
					RandomPointsBuilder builder = new RandomPointsBuilder(geometryFactory);
					builder.setExtent(perimeterGeometry);
					builder.setNumPoints(4);
					Geometry points = builder.getGeometry();
					LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>(Arrays.asList(points.getCoordinates()));
					coordinates.add(new Coordinate(0, 0));
					commander.drive(coordinates);
				}
				break;
			case CONTOUR_OBSTACLE:
				commander.makeRightSquare(event.getAngle());
				break;
			case FOLLOW_PATH:
				if (pathPoints != null)
					commander.drive(pathPoints);
				break;
		}
		for (StateChangeListener stateChangeListener : stateChangeListeners) {
			stateChangeListener.onStateChange(oldState, state, event);
		}
	}
	
	/**
	 * Calculate the heading variation to be passed to drive method of the commander. 
	 * @return Temporarily, it just returns a random number between 0 and 360 degrees.
	 * @see Commander#drive(float) 
	 * @see Commander#drive(float, float) 
	 */
	private float calculateDriveHeadingVariation(MappingEvent event){
		/*float maxHeading = 0;
		double maxArea = 0;
		for (float i = 0; i < 359; i++) {
			Coordinate dest = new Coordinate(
				event.getX() + Math.cos(i*Math.PI/180f) * 5 * parameters.getCommanderDriveDistance(),
				event.getY() + Math.sin(i*Math.PI/180f) * 5 * parameters.getCommanderDriveDistance()
			);
			Coordinate origin = new Coordinate(
				event.getX(),
				event.getY()
			);
			LineSegment lineSegment = new LineSegment(origin, dest);
			LineString lineString = lineSegment.toGeometry(geometryFactory);
			BufferParameters bufferParameters = new BufferParameters();
			bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
			BufferOp bufferOp = new BufferOp(lineString, bufferParameters);
			Geometry g = bufferOp.getResultGeometry(parameters.getRunnerWidth());
			if (!perimeterGeometry.isEmpty()){
				g = g.intersection(perimeterGeometry);
			}
			g = g.difference(freeGeometry);
			double area = g.getArea();
			if (area > maxArea) {
				maxArea = area;
				maxHeading = i;
			}
		}
		System.out.println("maxArea = " + maxArea);
		System.out.println("maxHeading = " + maxHeading);
		float current = commander.getCurrentHeading();
		current = current - maxHeading;
		return current;*/
		return 90 + random.nextFloat()*180;
	}
	
	private LineString generatePathGeometry(Coordinate origin, ArrayList<Coordinate> points) {
		Coordinate[] coordinates = new Coordinate[points.size() + 1];
		coordinates[0] = origin;
		for (int i = 1; i <= points.size(); i++) {
			coordinates[i] = points.get(i-1);
		}
		CoordinateSequence coords = new CoordinateArraySequence(coordinates);
		return new LineString(coords, geometryFactory);
	}
	
	private MultiLineString generateEdgesGeometry(ArrayList<Coordinate[]> edges) {
		LineString[] lineStrings = new LineString[edges.size()];
		for (int i = 0; i < edges.size(); i++) {
			Coordinate[] coordinates = {edges.get(i)[0], edges.get(i)[1]};
			CoordinateSequence coords = new CoordinateArraySequence(coordinates);
			LineString line = new LineString(coords, geometryFactory);
			lineStrings[i] = line;
		}
		return new MultiLineString(lineStrings, geometryFactory);
	}
	
	private Geometry filterGeometry(Geometry g, MappingEvent event) {
		double totalArea = perimeterGeometry.getArea() - objectsGeometry.getArea();
		double threshold = totalArea / 100;
		Coordinate coordinate = new Coordinate(event.getX(), event.getY());
		Point point = geometryFactory.createPoint(coordinate);
		Geometry r = geometryFactory.createMultiPolygon(null);
		//double largerArea = 0;
		double shortestDistance = Double.MAX_VALUE;
		Geometry betterGeometry = null;
		for (int i = 0; i < g.getNumGeometries(); i++) {
			Geometry temp = g.getGeometryN(i);
			if (temp.getGeometryType().equals("Polygon")) {
				if (temp.getArea() > threshold) {
					//if (temp.getArea() > largerArea) {
					if (temp.distance(point) < shortestDistance) {
						//largerArea = temp.getArea();
						shortestDistance = temp.distance(point);
						betterGeometry = temp;
					}
					//r = r.union(temp);
				}
			}
		}
		r = betterGeometry;
		return r;
	}
	
	private Coordinate calculateDestination(MappingEvent event) {
		try {
			Geometry undiscoveredArea = perimeterGeometry.difference(freeGeometry);
			undiscoveredArea = undiscoveredArea.difference(objectsGeometry);
			Coordinate coordinate = new Coordinate(event.getX(), event.getY());
			Point point = geometryFactory.createPoint(coordinate);
			randomPointsBuilder.setNumPoints(10);
			Geometry ext = filterGeometry(undiscoveredArea, event);
			if (ext == null) return null;
			randomPointsBuilder.setExtent(ext);
			MultiPoint points = (MultiPoint) randomPointsBuilder.getGeometry();
			Point betterPoint = (Point) points.getGeometryN(0);
			double shortestDistance = Double.MAX_VALUE;
			for (int i = 0; i < points.getNumGeometries(); i++) {
				Point point2 = (Point) points.getGeometryN(i);
				if (point.distance(point2) < shortestDistance) {
					betterPoint = point2;
				}
			}
			Coordinate coordinate2 = betterPoint.getCoordinate();
			return coordinate2;
		} catch (TopologyException e) {
			System.err.println("Topology exception.");
			return new Coordinate(0,0);
		}
	}
	
	private ArrayList<Coordinate> generateRandomPoints(int n) {
		randomPointsBuilder.setNumPoints(n);
		randomPointsBuilder.setExtent(perimeterGeometry);
		MultiPoint points = (MultiPoint) randomPointsBuilder.getGeometry();
		ArrayList<Coordinate> arrayOfCoord = new ArrayList<Coordinate>();
		for (int i = 0; i < points.getNumGeometries(); i++) {
			Point p = (Point) points.getGeometryN(i);
			arrayOfCoord.add(p.getCoordinate());
		}
		return arrayOfCoord;
	}
	
	private Coordinate selectVertice(ArrayList<Coordinate> Q,
			ArrayList<Coordinate> R, double[] dist, boolean[] visited) {
		
		double leastDistance = Double.MAX_VALUE;
		Coordinate leastElement = null;
		for (Coordinate v : Q) {
			int vIndex = R.indexOf(v);
			if (dist[vIndex] < leastDistance && !visited[vIndex]) {
				leastDistance = dist[vIndex];
				leastElement = v;
			}
		}
		if (leastElement == null) {
			System.err.println("leastElement == null");
		}
		return leastElement;
	}
	
	private ArrayList<Coordinate> getNeighbors(Coordinate u, ArrayList<Coordinate[]> R) {
		ArrayList<Coordinate> neighbors = new ArrayList<Coordinate>();
		for (Coordinate[] tuple : R) {
			if (tuple.length == 2) {
				if (tuple[0].equals(u)) {
					if (!neighbors.contains(tuple[1])) {
						neighbors.add(tuple[1]);
					}
				}
			}
		}
		return neighbors;
	}
	
	private double distance(Coordinate u, Coordinate v) {
		return u.distance(v);
	}
	
	private ArrayList<Coordinate> makePath(Coordinate origin, Coordinate destination) {
		ArrayList<Coordinate> randomPoints = generateRandomPoints(10);
		randomPoints.add(origin);
		randomPoints.add(destination);
		ArrayList<Coordinate[]> edges = new ArrayList<Coordinate[]>();
		for (int i = 0; i < randomPoints.size(); i++) {
			for (int j = 0; j < randomPoints.size(); j++) {
				if (i == j) continue;
				Coordinate[] coordinates = new Coordinate[2];
				coordinates[0] = randomPoints.get(i);
				coordinates[1] = randomPoints.get(j);
				CoordinateSequence coords = new CoordinateArraySequence(coordinates);
				LineString line = new LineString(coords, geometryFactory);
				if (!line.intersects(objectsGeometry)) {
					edges.add(coordinates);
				}
			}
		}
		edgesGeometry = generateEdgesGeometry(edges);
		double[] dist = new double[randomPoints.size()];
		boolean[] visited = new boolean[randomPoints.size()];
		int[] previous = new int[randomPoints.size()];
		for (int i = 0; i < randomPoints.size(); i++) {
			dist[i] = Double.MAX_VALUE;
			visited[i] = false;
			previous[i] = -1;
		}
		dist[10] = 0;
		ArrayList<Coordinate> Q = new ArrayList<Coordinate>();
		Q.add(origin);
		
		while (!Q.isEmpty()) {
			System.out.println("Here");
			Coordinate u = selectVertice(Q, randomPoints, dist, visited);
			if (u == null) break;
			int uIndex = randomPoints.indexOf(u);
			System.out.println("Selected: " + uIndex);
			Q.remove(u);
			visited[uIndex] = true;
			ArrayList<Coordinate> neighbors = getNeighbors(u, edges);
			System.out.print("Neighbors: ");
			for (Coordinate c : neighbors) {
				System.out.print(randomPoints.indexOf(c) + ",");
			}
			for (int i = 0; i < neighbors.size(); i++) {
				Coordinate v = neighbors.get(i);
				int vIndex = randomPoints.indexOf(v);
				double alt = dist[uIndex] + distance(u, v);
				System.out.println("Dist to " + vIndex + ": "+ alt);
				if (alt < dist[vIndex]) {
					dist[vIndex] = alt;
					previous[vIndex] = uIndex;
					if (!visited[vIndex]) {
						Q.add(randomPoints.get(vIndex));
					}
				}
				
			}
		}
		
		ArrayList<Coordinate> path = new ArrayList<Coordinate>();
		Coordinate u = destination;
		int uIndex = randomPoints.indexOf(u);
		for (int i = 0; i < previous.length; i++) {
			System.out.println("[" + i + "] = " + previous[i]);
		}
		System.out.println("uIndex = " + uIndex);
		while (previous[uIndex] != -1) {
			System.out.println(uIndex + "->");
			path.add(0,u);
			u = randomPoints.get(previous[uIndex]);
			uIndex = randomPoints.indexOf(u);
		}
		path.add(0, u);
		return path;
	}
	
	private ArrayList<Coordinate> calculatePathPoints(MappingEvent event) {
		Coordinate origin = new Coordinate(event.getX(), event.getY());
		Coordinate destination = calculateDestination(event);
		if (destination == null) return null;
		Coordinate[] coordinates = {origin, destination};
		CoordinateSequence coords = new CoordinateArraySequence(coordinates);
		LineString line = new LineString(coords, geometryFactory);
		ArrayList<Coordinate> list;
		if (line.intersects(objectsGeometry)) {
			System.out.println("Intersects.");
			list = makePath(origin, destination);
		} else {
			System.out.println("DOES NOT intersect.");
			list = new ArrayList<Coordinate>();
			list.add(destination);
		}
		pathGeometry = generatePathGeometry(origin, list);
		return list;
	}
	
	private boolean pointNotVisited(MappingEvent event) {
		Coordinate coordinate = new Coordinate(event.getX(), event.getY());
		Point p = geometryFactory.createPoint(coordinate);
		return !p.within(freeGeometry);
	}
	
	/**
	 * Tells is the mapping is done.
	 * @return <code>true</code> if the mapping is done and <code>false</code> otherwise. It's just returning false currently.
	 */
	private boolean isDone(){
		if(perimeterGeometry.isEmpty()) return false;
		double area = perimeterGeometry.difference(freeGeometry.union(objectsGeometry)).getArea();
		//area = area / ((1/4)*Math.PI * getParameters().getRunnerWidth()*getParameters().getRunnerWidth());
		System.out.println("Unknown area: " + area);
		System.out.println("Robot area: " + (((1d/4d) * Math.PI * getParameters().getRunnerWidth()*getParameters().getRunnerWidth())));
		return false;
	}

	/**
	 * 
	 * @return The current state of the algorithm.
	 */
	public State getState() {
		return state;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}
	
	private Geometry generateDistanceReachedGeometry(MappingEvent event){
		LineSegment lineSegment = new LineSegment(lastEvent.getX(), lastEvent.getY(), event.getX(), event.getY());
		LineString lineString = lineSegment.toGeometry(geometryFactory);
		BufferParameters bufferParameters = new BufferParameters();
		bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		BufferOp bufferOp = new BufferOp(lineString, bufferParameters);
		return bufferOp.getResultGeometry(parameters.getRunnerWidth());
	}

	public Geometry getObjectsGeometry() {
		return objectsGeometry;
	}

	public Geometry getFreeGeometry() {
		return freeGeometry;
	}

	public Geometry getPerimeterGeometry() {
		return perimeterGeometry;
	}
	
	public Geometry getPathGeometry() {
		return pathGeometry;
	}
	
	public Geometry getEdgesGeometry() {
		return edgesGeometry;
	}
	
	public void eraseEdgesGeometry() {
		edgesGeometry = geometryFactory.createGeometry(null);
	}
	
	public void addStateChangeListener(StateChangeListener listener){
		if(!stateChangeListeners.contains(listener)) stateChangeListeners.add(listener);
	}
	
	private static void mappingEventNotExpected(MappingEvent event){
		System.err.println("Mapping event of type " + event.getType().name() + " not expected. Point: (" + event.getX() + ", " + event.getY() + ").");
	}
}

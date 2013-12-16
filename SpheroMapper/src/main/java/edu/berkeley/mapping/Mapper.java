package edu.berkeley.mapping;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;
import com.vividsolutions.jtstest.function.CreateRandomShapeFunctions;
import com.vividsolutions.jtstest.function.CreateShapeFunctions;
import java.util.ArrayList;
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
	private ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<StateChangeListener>();
	
	/**
	 * A random number generator to be used temporarily in <code>calculateDriveHeadingVariation</code> method.
	 * @see #calculateDriveHeadingVariation() 
	 */
	private final Random random = new Random();
	
	/**
	 * The geometry factory for the mapping process.
	 */
	private GeometryFactory geometryFactory = new GeometryFactory();
	
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
	
	/**
	 * This method must be called by the "robot side" implementation to report
	 * any relevant event to the mapper.
	 * @param event The event reported.
	 */
	public void reportEvent(MappingEvent event){
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
						freeGeometry = freeGeometry.union(g);
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
							obstaclePoints.add(new Coordinate(
									event.getX() + Math.cos(event.getAngle()*Math.PI/180f),
									event.getY() + Math.sin(event.getAngle()*Math.PI/180f)
							));
							setState(State.CONTOUR_OBSTACLE, event);
						} else {
							System.out.println("Point VISITED.");
							if (perimeterGeometry.isEmpty()) {
								headingVariation = calculateDriveHeadingVariation(event);
								setState(State.SEARCH_OBSTACLE, event);
							} else {
								pathPoints = calculatePathPoints(event);
								setState(State.FOLLOW_PATH, event);
							}
						}
						break;
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
						obstaclePoints.add(new Coordinate(
							event.getX() + Math.cos(event.getAngle()*Math.PI/180f),
							event.getY() + Math.sin(event.getAngle()*Math.PI/180f)
						));
						if(possibleEndPoint.distance(contourStartPoint) <= parameters.getContourFinishPointRadiusThreshold()){
							obstaclePoints.add(obstaclePoints.get(0));
							Polygon obstacle = geometryFactory.createPolygon(obstaclePoints.toArray(new Coordinate[obstaclePoints.size()]));
							Point p = geometryFactory.createPoint(possibleEndPoint);
							if(p.within(obstacle)){
								System.out.println("Perimeter!!!");
								perimeterGeometry = perimeterGeometry.union(obstacle);
							}else{
								objectsGeometry = objectsGeometry.union(obstacle);
							}
							if (perimeterGeometry.isEmpty()) {
								headingVariation = calculateDriveHeadingVariation(event);
								setState(State.SEARCH_OBSTACLE, event);
							} else {
								pathPoints = calculatePathPoints(event);
								setState(State.FOLLOW_PATH, event);
							}
						}else{
							setState(State.CONTOUR_OBSTACLE, event);
						}
						break;
					default:
						mappingEventNotExpected(event);
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
				break;
			case CONTOUR_OBSTACLE:
				commander.makeRightSquare(event.getAngle());
				break;
			case FOLLOW_PATH:
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
				event.getX() + Math.cos(i*Math.PI/180f) * parameters.getCommanderDriveDistance(),
				event.getY() + Math.sin(i*Math.PI/180f) * parameters.getCommanderDriveDistance()
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
		return maxHeading;*/
		return 90 + random.nextFloat()*180;
	}
	
	private Coordinate calculateDestination() {
		Geometry undiscoveredArea = perimeterGeometry.difference(freeGeometry);
		undiscoveredArea = undiscoveredArea.difference(objectsGeometry);
		randomPointsBuilder.setNumPoints(1);
		randomPointsBuilder.setExtent(undiscoveredArea);
		MultiPoint points = (MultiPoint) randomPointsBuilder.getGeometry();
		Point point = (Point) points.getGeometryN(0);
		Coordinate coordinate = point.getCoordinate();
		return coordinate;
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
	
	private Coordinate selectVertice(ArrayList<Coordinate> Q, ArrayList<Coordinate[]> E,
			ArrayList<Coordinate> R, double[] dist, boolean[] visited) {
		
		double leastDistance = 1000000;
		Coordinate leastElement = null;
		for (int i = 0; i < Q.size(); i++) {
			Coordinate v = Q.get(i);
			int vIndex = R.indexOf(v);
			if (dist[vIndex] < leastDistance && !visited[vIndex]) {
				leastDistance = dist[vIndex];
				leastElement = v;
			}
		}
		return leastElement;
	}
	
	private ArrayList<Coordinate> getNeighbors(Coordinate u, ArrayList<Coordinate[]> R) {
		ArrayList<Coordinate> neighbors = new ArrayList<Coordinate>();
		for (int i = 0; i < R.size(); i++) {
			Coordinate[] tuple = R.get(i);
			if (tuple[0].equals(u)) {
				if (!neighbors.contains(u)) {
					neighbors.add(u);
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
				Coordinate[] coordinates = {randomPoints.get(i), randomPoints.get(j)};
				CoordinateSequence coords = new CoordinateArraySequence(coordinates);
				LineString line = new LineString(coords, geometryFactory);
				if (!line.intersects(objectsGeometry)) {
					edges.add(coordinates);
				}
			}
		}
		double[] dist = new double[randomPoints.size()];
		boolean[] visited = new boolean[randomPoints.size()];
		int[] previous = new int[randomPoints.size()];
		for (int i = 0; i < randomPoints.size(); i++) {
			dist[i] = 1000000;
			visited[i] = false;
			previous[i] = -1;
		}
		dist[10] = 0;
		ArrayList<Coordinate> Q = new ArrayList<Coordinate>();
		Q.add(origin);
		
		while (!Q.isEmpty()) {
			Coordinate u = selectVertice(Q, edges, randomPoints, dist, visited);
			int uIndex = randomPoints.indexOf(u);
			if (u.equals(destination)) {
				break;
			}
			Q.remove(u);
			visited[uIndex] = true;
			ArrayList<Coordinate> neighbors = getNeighbors(u, edges);
			for (int i = 0; i < neighbors.size(); i++) {
				Coordinate v = neighbors.get(i);
				int vIndex = randomPoints.indexOf(v);
				double alt = dist[vIndex] + distance(u, v);
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
		while (previous[uIndex] != -1) {
			path.add(0,u);
			u = randomPoints.get(previous[uIndex]);
			uIndex = randomPoints.indexOf(u);
		}
		path.add(0, u);
		
		return path;
	}
	
	private ArrayList<Coordinate> calculatePathPoints(MappingEvent event) {
		Coordinate origin = new Coordinate(event.getX(), event.getY());
		Coordinate destination = calculateDestination();
		Coordinate[] coordinates = {origin, destination};
		CoordinateSequence coords = new CoordinateArraySequence(coordinates);
		LineString line = new LineString(coords, geometryFactory);
		ArrayList<Coordinate> list;
		if (line.intersects(objectsGeometry)) {
			list = makePath(origin, destination);
		} else {
			list = new ArrayList<Coordinate>();
			list.add(destination);
		}
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
	
	public void addStateChangeListener(StateChangeListener listener){
		if(!stateChangeListeners.contains(listener)) stateChangeListeners.add(listener);
	}
	
	private static void mappingEventNotExpected(MappingEvent event){
		System.err.println("Mapping event of type " + event.getType().name() + " not expected. Point: (" + event.getX() + ", " + event.getY() + ").");
	}
}

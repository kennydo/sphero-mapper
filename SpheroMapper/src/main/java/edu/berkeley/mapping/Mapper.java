package edu.berkeley.mapping;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
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
	 * The geometry object to represent the objects on the ground.
	 */
	private Geometry objectsGeometry = geometryFactory.createMultiPolygon(null);
	
	/**
	 * The geometry object to represent the free ground, that is, the areas where there's nothing on.
	 */
	private Geometry freeGeometry = geometryFactory.createMultiPolygon(null);
	
	private Geometry perimeterGeometry = geometryFactory.createGeometry(null);
	
	/**
	 * Last event reported.
	 */
	private MappingEvent lastEvent;
	
	/**
	 * The start point of the contour process
	 */
	private final Coordinate contourStartPoint = new Coordinate();
	
	private final ArrayList<Coordinate> obstaclePoints = new ArrayList<>();
	
	/**
	 * The heading variation to be set if the state needs to use the drive command.
	 * @see #setState(edu.berkeley.mapping.Mapper.State, edu.berkeley.mapping.MappingEvent) 
	 */
	private float headingVariation;
	

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
						contourStartPoint.setOrdinate(Coordinate.X, event.getX());
						contourStartPoint.setOrdinate(Coordinate.Y, event.getY());
						obstaclePoints.clear();
						obstaclePoints.add(new Coordinate(
								event.getX() + Math.cos(event.getAngle()*Math.PI/180f),
								event.getY() + Math.sin(event.getAngle()*Math.PI/180f)
						));
						setState(State.CONTOUR_OBSTACLE, event);
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
							}else{
								objectsGeometry = objectsGeometry.union(obstacle);
							}
							headingVariation = calculateDriveHeadingVariation();
							setState(State.SEARCH_OBSTACLE, event);
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
	private float calculateDriveHeadingVariation(){
		return 90 + random.nextFloat()*180;
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

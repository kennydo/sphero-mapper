package edu.berkeley.mapping;

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
		 * The state of the mapping algorithm in which the robot must search for obstacles.
		 */
		SEARCH_OBSTACLE,
		/**
		 * The state of the mapping algorithm in which the robot must try to contour the obstacle that was found.
		 */
		CONTOUR_OBSTACLE
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
	private State state = State.SEARCH_OBSTACLE;
	
	/**
	 * A random number generator to be used temporarily in <code>calculateDriveHeadingVariation</code> method.
	 * @see #calculateDriveHeadingVariation() 
	 */
	private final Random random = new Random();

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
			case SEARCH_OBSTACLE:
				switch(event.getType()){
					case DISTANCE_REACHED:
						
						break;
					case COLLISION:
						state = State.CONTOUR_OBSTACLE;
						break;
					default:
						throw new RuntimeException("Mapping event not expected.");
				}
				break;
			case CONTOUR_OBSTACLE:
				switch(event.getType()){
					case SQUARE_COMPLETED:
						break;
					case COLLISION:
						break;
					default:
						throw new RuntimeException("Mapping event not expected.");
				}
				break;
		}
	}
	
	/**
	 * Calculate the heading variation to be passed to drive method of the commander. 
	 * @return Temporarily, it just returns a random number between 0 and 360 degrees.
	 * @see Commander#drive(float) 
	 * @see Commander#drive(float, float) 
	 */
	private float calculateDriveHeadingVariation(){
		return random.nextFloat()*360;
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
}

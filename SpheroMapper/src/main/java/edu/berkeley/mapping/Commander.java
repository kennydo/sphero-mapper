package edu.berkeley.mapping;

/**
 * Command interface for the mapping algorithm.
 * The mapping algorithm user must implement the methods defined in this
 * interface, since the mapper will use them.
 * @version 1.0
 * @see Mapping
 */
public interface Commander {
	/**
	 * Command the robot to drive a given distance.
	 * @param headingVariation The angle variation of the current heading of the
	 * robot.
	 * The robot must change its heading to the new angle, which is the current
	 * plus the variation.
	 * @param distance The distance to be traveled.
	 */
	public void drive(float headingVariation, float distance);
	/**
	 * Command the robot to drive until it has a collision.
	 * @param headingVariation The angle variation of the current heading of the
	 * robot.
	 * The robot must change its heading to the new angle, which is the current
	 * plus the variation.
	 */
	public void drive(float headingVariation);
	/**
	 * Command the robot to make a square by first going backwards, then to its
	 * left, forwards, and finally to right.
	 * If the square is finished, it should report a MappingEvent of type
	 * SQUARE_COMPLETED.
	 * If a collision is detected before the square is completed, then a
	 * MappingEvent of type COLLISION must be reported.
	 * @see Mapping
	 */
	public void makeLeftSquare();
	/**
	 * Command the robot to make a square by first going backwards, then to its
	 * right, forwards, and finally to left.
	 * If the square is finished, it should report a MappingEvent of type
	 * SQUARE_COMPLETED.
	 * If a collision is detected before the square is completed, then a
	 * MappingEvent of type COLLISION must be reported.
	 * @see Mapping
	 */
	public void makeRightSquare();
	/**
	 * Command the robot to stop, which means the end of the algorithm.
	 */
	public void stop();
}

package edu.berkeley.mapping;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.List;

/**
 * Command interface for the mapping algorithm.
 * The mapping algorithm user must implement the methods defined in this
 * interface, since the mapper will use them.
 * @version 1.0
 * @see Mapper
 */
public interface Commander {
	/**
	 * Command the robot to drive a given distance.
	 * @param headingVariation The angle variation of the current heading of the
	 * robot. Relative to the current heading and increasing in counter-clockwise fashion
	 * The robot must change its heading to the new angle, which is the current
	 * plus the variation.
	 * @param distance The distance to be traveled.
	 */
	public void drive(float headingVariation, float distance);
	/**
	 * Command the robot to drive until it has a collision.
	 * @param headingVariation The angle variation of the current heading of the
	 * robot. Relative to the current heading and increasing in counter-clockwise fashion
	 * The robot must change its heading to the new angle, which is the current
	 * plus the variation.
	 */
	public void drive(float headingVariation);

    /**
     * Command the robot to drive in order to each point in points.
     * Reports will be made after traveling to each point.  If a collision is detected,
     * an event will be reported and the sphero will be stopped.
     * @param points The points to travel to.
     */
    public void drive(List<Coordinate> points);

    /**
     * Command the robot to make a square by first going backwards, then to its
     * left, forwards, and finally to right.
     * If the square is finished, it should report a MappingEvent of type
     * The robot will rotate to face the collisionAngle parameter before making the left square.
     * @param collisionAngle The angle where the collision occurred.  Will be given
     *                       in absolute.  Increasing angle goes in counter-clockwise motion.
     */
    public void makeLeftSquare(float collisionAngle);

    /**
     * Command the robot to make a square by first going backwards, then to its
     * right, forwards, and finally to left.
     * If the square is finished, it should report a MappingEvent of type
     * SQUARE_COMPLETED.
     * If a collision is detected before the square is completed, then a
     * MappingEvent of type COLLISION must be reported.
     * The robot will rotate to face the collisionAngle parameter before making the left square.
     * @param collisionAngle The angle where the collision occurred.  Will be given
     *                       in absolute.  Increasing angle goes in counter-clockwise motion.
     */
    public void makeRightSquare(float collisionAngle);

	/**
	 * Command the robot to stop, which means the end of the algorithm.
	 */
	public void stop();

    /**
     * Gets the current heading reported from the commander.
     * @return the current heading
     */
    public float getCurrentHeading();
}

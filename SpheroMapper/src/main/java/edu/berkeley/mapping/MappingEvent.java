/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping;

/**
 * Class used to describe any event for the mapping algorithm.
 */
public class MappingEvent {
	/**
	 * Defines a collision event.
	 *	- (x,y) is the position where the collision occurred.
	 *  - angle is the angle variation of where, in the robot, the collision
	 * occurred and the robot's heading.
	 */
	public static final int COLLISION = 1;
	/**
	 * This type of event must be reported when the robot reaches the distance
	 * given to the Commander in method drive.
	 *	- (x,y) is the robot's position in the moment this event occurred.
	 *  - angle is the robot's heading.
	 * occurred and the robot's heading.
	 * @see Commander
	 */
	public static final int DISTANCE_REACHED = 2;
	/**
	 * This type of event must be reported when the robot completes a square
	 * without a collision.
	 *	- (x,y) is the robot's position in the moment this event occurred.
	 *	- angle is the robot's heading.
	 */
	public static final int SQUARE_COMPLETED = 3;
	
	/**
	 * The event type.
	 * Possible values:
	 *	- COLLISION
	 *	- DISTANCE_REACHED
	 *	- SQUARE_COMPLETED
	 */
	private int type;
	
	/**
	 * Variable representing the x position of the robot.
	 * See the event types constants for more information.
	 */
	private float x;
	
	/**
	 * Variable representing the y position of the robot.
	 * See the event types constants for more information.
	 */
	private float y;
	
	/**
	 * Variable representing a angle, its meaning depends on the event type.
	 * See the event types constants for more information.
	 */
	private float angle;

	
	public MappingEvent(int type, float x, float y) {
		this.type = type;
		this.x = x;
		this.y = y;
	}

	public MappingEvent(int type, float x, float y, float angle) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.angle = angle;
	}

	/**
	 * 
	 * @return The event type.
	 * Possible values:
	 *	- COLLISION
	 *	- DISTANCE_REACHED
	 *	- SQUARE_COMPLETED
	 */
	public int getType() {
		return type;
	}

	/**
	 * 
	 * @return Returns x position.
	 * See the event types constants for more information.
	 */
	public float getX() {
		return x;
	}

	/**
	 * 
	 * @return Returns y position.
	 * See the event types constants for more information.
	 */
	public float getY() {
		return y;
	}

	/**
	 * 
	 * @return Returns an angle.
	 * See the event types constants for more information.
	 */
	public float getAngle() {
		return angle;
	}
}

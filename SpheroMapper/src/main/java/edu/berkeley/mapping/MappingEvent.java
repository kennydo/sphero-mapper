/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.berkeley.mapping;

/**
 * Class used to describe any event for the mapping algorithm.
 * Changes revision:
 * <ul>
 *	<li>1.1
 *		<ul>
 *			<li>Changed event type constants to be a enumeration.</li>
 *		</ul>
 *	</li>
 * </ul>
 * @version 1.1
 */
public class MappingEvent {
	/**
	 * Enumeration listing all possible event types.
	 */
	public static enum Type{
		/**
		 * Defines a collision event.
		 *	- (x,y) is the position where the collision occurred.
		 *  - angle is the angle variation of where, in the robot, the collision
		 * occurred and the robot's heading.
		 */
		COLLISION,
		/**
		 * This type of event must be reported when the robot reaches the distance
		 * given to the Commander in method drive.
		 *	- (x,y) is the robot's position in the moment this event occurred.
		 *  - angle is the robot's heading.
		 * occurred and the robot's heading.
		 * @see Commander#drive(float, float) 
		 */
		DISTANCE_REACHED,
		/**
		 * This type of event must be reported when the robot completes a square
		 * without a collision.
		 *	- (x,y) is the robot's position in the moment this event occurred.
		 *	- angle is the robot's heading.
		 */
		SQUARE_COMPLETED
	}
	
	/**
	 * This event's type.
	 */
	private Type type;
	
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

	
	public MappingEvent(Type type, float x, float y) {
		this.type = type;
		this.x = x;
		this.y = y;
	}

	public MappingEvent(Type type, float x, float y, float angle) {
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
	public Type getType() {
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

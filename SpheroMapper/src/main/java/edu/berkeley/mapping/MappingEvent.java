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
 *	<li>1.12
 *		<ul>
 *			<li>Added event type START.</li>
 *		</ul>
 *	</li>
 * </ul>
 * @version 1.12
 */
public class MappingEvent {
	/**
	 * Enumeration listing all possible event types.
	 */
	public static enum Type{
		/**
		 * This tells the algorithm to begin.
		 *	- (x,y) is the position the algorithm must consider the robot is at the beginning of the process.
		 */
		START,
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
		SQUARE_COMPLETED,
        /**
         * This type of event must be reported when the robot completes traveling
         * to an internmediate point within the list of points.
         *	- (x,y) is the robot's position in the moment this event occurred.
         */
        POINT_REACHED,
        /**
         * This type of event must be reported when the robot completes traveling
         * the list of points.
         *	- (x,y) is the robot's position in the moment this event occurred.
         */
        POINTS_COMPLETED
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

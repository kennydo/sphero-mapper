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
	 * The event type.
	 * Possible values:
	 *	- COLLISION
	 *	- DISTANCE_REACHED
	 *	- SQUARE_COMPLETED
	 */
	private MappingEventType type;
	
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

	
	public MappingEvent(MappingEventType type, float x, float y) {
		this.type = type;
		this.x = x;
		this.y = y;
	}

	public MappingEvent(MappingEventType type, float x, float y, float angle) {
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
	public MappingEventType getType() {
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

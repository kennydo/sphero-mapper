package edu.berkeley.mapping;

/**
 * Created by Brandon on 11/26/13.
 */
public enum MappingEventType {

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
     * @see Commander
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

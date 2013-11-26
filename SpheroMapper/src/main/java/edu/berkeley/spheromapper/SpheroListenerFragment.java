package edu.berkeley.spheromapper;

import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.base.Robot;
import orbotix.robot.sensor.DeviceSensorsData;

/**
 * Created by kedo on 11/25/13.
 */
public interface SpheroListenerFragment {
    /*
    These are the methods that Fragments must implement if they want to listen to Sphero's updates,
    since the sphero's updates are sent from the MainActivity to each subordinate Fragment
     */


    /*
    This data is polled, so it will happen frequently.
     */
    public void onSpheroLocationUpdate(DeviceSensorsData sensorsData);

    /*
    Collisions are detected asynchronously
     */
    public void onSpheroCollision(CollisionDetectedAsyncData collisionData);

    /*
    We call this method whenever the sphero is connected.
    Note that this may happen because the sphero lost connection.
    We'll probably want to reset state whenever this is called.
    Also, all calls to onLocationUpdate and onCollision will only happen we pass a valid
    sphero through onConnected to the fragment
     */
    public void onSpheroConnected(Robot sphero);
}

package edu.berkeley.spheromapper;

import java.util.List;

import orbotix.robot.sensor.LocatorData;

/**
 * Created by kedo on 11/25/13.
 */
public interface CollisionLocationHistoryProvider {
    public List<LocatorData> getCollisionLocations();
}

package edu.berkeley.spheromapper;

import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.mapping.Commander;
import edu.berkeley.mapping.MappingEvent;
import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;

/**
 * Created by Brandon on 11/26/13.
 */
public class SpheroCommander implements Commander{

    private Sphero sphero;
    private float currentHeading = 0.0f;
    private static final float SQUARE_LENGTH = 25.0f;
    private static final float DEFAULT_DRIVE_SPEED = 0.25f;
    private boolean collisionDetected;
    private float collisionAngle;
    private boolean distanceMade;
    private DistanceTraveledListener distanceTraveledListener;
    private SquareTraveledListener squareTraveledListener;
    private PointsTraveledListener pointsTraveledListener;

    private enum DRIVE_TRANSITION_STATE {
        INIT, DRIVE
    }

    public SpheroCommander(Sphero sphero) {
        this.sphero = sphero;
        collisionDetected = false;
        sphero.getCollisionControl().addCollisionListener(new CollisionReportListener());
        distanceTraveledListener = new DistanceTraveledListener();
        squareTraveledListener = new SquareTraveledListener();
        pointsTraveledListener = new PointsTraveledListener();
        sphero.getSensorControl().addSensorListener(distanceTraveledListener, SensorFlag.ATTITUDE, SensorFlag.LOCATOR);
        sphero.getSensorControl().addSensorListener(squareTraveledListener, SensorFlag.ATTITUDE, SensorFlag.LOCATOR);
        sphero.getSensorControl().addSensorListener(pointsTraveledListener, SensorFlag.ATTITUDE, SensorFlag.LOCATOR);
    }

    private synchronized void drive(float headingVariation, float distance, boolean isReportFinish) {
        sphero.stop();
        disableListeners();
        Log.d("DRIVING", "Starting to drive in " + headingVariation + " for " + distance);
        currentHeading = (currentHeading - headingVariation) % 360;
        if (currentHeading < 0) {
            currentHeading += 360;
        }
        sphero.rotate(headingVariation);
        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
        distanceTraveledListener.initialize(distance, isReportFinish);
    }

    @Override
    public void drive(float headingVariation, float distance) {
        drive(headingVariation, distance, true);
    }

    @Override
    public void drive(float headingVariation) {
        drive(headingVariation, Float.MAX_VALUE);
    }

    public void drive(List<Coordinate> points) {
        pointsTraveledListener.initialize(points);
        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
    }

    public void makeLeftSquare() {
        sphero.stop();
        disableListeners();
        currentHeading = (currentHeading + 180.0f) % 360;
        sphero.rotate(180.0f);
        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
        squareTraveledListener.initialize(true);
    }

    @Override
    public void makeLeftSquare(float collisionAngle) {
        sphero.rotate(collisionAngle);
        makeLeftSquare();
    }

    public void makeRightSquare() {
        sphero.stop();
        disableListeners();
        currentHeading = (currentHeading + 180.0f) % 360;
        sphero.rotate(180.0f);
        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
        squareTraveledListener.initialize(false);
    }

    @Override
    public void makeRightSquare(float collisionAngle) {
        sphero.rotate(collisionAngle);
        makeRightSquare();
    }

    @Override
    public void stop() {
        sphero.stop();
    }

    @Override
    public float getCurrentHeading() {
        return currentHeading;
    }

    public static float distanceTraveled(float startX, float startY, float endX, float endY) {
        return (float) Math.pow(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2), 0.5);
    }

    private void processCollision(float x, float y) {
        Log.d("Collision", "Collision has been found!");
        MappingEvent collisionEvent = new MappingEvent(MappingEvent.Type.COLLISION, x, y, collisionAngle);
        Log.d("SpheroCommander", "Reporting event " + collisionEvent.getType().name() + " at ("+ x + ", " + y + ") with angle=" + collisionAngle);
        finish();
        Runner.getMapper().reportEvent(collisionEvent);
        collisionDetected = false;
    }

    private void processSquareSuccess(float x, float y) {
        processEvent(x, y, MappingEvent.Type.SQUARE_COMPLETED);
    }

    private void processDistanceSuccess(float x, float y) {
        processEvent(x, y, MappingEvent.Type.DISTANCE_REACHED);
    }

    private void processPointsSuccess(float x, float y) {
        processEvent(x, y, MappingEvent.Type.POINTS_COMPLETED);
    }

    private void processIntermediatePointSuccess(float x, float y) {
        processEvent(x, y, MappingEvent.Type.POINT_REACHED);
    }

    private synchronized void processEvent(float x, float y, MappingEvent.Type event) {
        MappingEvent collisionEvent = new MappingEvent(event, x, y);
        Log.d("SpheroCommander", "Reporting event " + event.name() + " at ("+ x + ", " + y + ")");
        finish();
        Runner.getMapper().reportEvent(collisionEvent);
    }

    private void finish() {
        Log.d("SUCCESS", "Finished event");
        disableListeners();
        sphero.stop();
    }

    private void disableListeners() {
        distanceTraveledListener.disable();
        squareTraveledListener.disable();
        pointsTraveledListener.disable();
    }

    private class DistanceTraveledListener implements SensorListener {

        private float distanceToTravel;
        private DRIVE_TRANSITION_STATE currentState;
        private float startX;
        private float startY;
        private boolean isReportFinish;
        private volatile boolean isEnabled;

        public DistanceTraveledListener() {
            isEnabled = false;
        }

        public void initialize(float distanceToTravel, boolean isReportFinish) {
            this.distanceToTravel = distanceToTravel;
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
            this.isReportFinish = isReportFinish;
            distanceMade = false;
            isEnabled = true;
            Log.d("INIT", "initializing listener");
        }

        @Override
        public synchronized void sensorUpdated(DeviceSensorsData deviceSensorsData) {
            if (isEnabled && deviceSensorsData != null && deviceSensorsData.getLocatorData() != null && deviceSensorsData.getLocatorData().getPosition() != null) {
                //currentHeading = deviceSensorsData.getAttitudeData().yaw;
                //while (currentHeading < 0) {
                //    currentHeading += 360;
                //}
                float currX = deviceSensorsData.getLocatorData().getPositionX();
                float currY = deviceSensorsData.getLocatorData().getPositionY();
                Log.d("Drive updated", "currX = " + currX + ", currY = " + currY);
                if (collisionDetected) {
                    Log.d("INIT", "CollisionDetected");
                    isEnabled = false;
                    processCollision(currX, currY);
                    return;
                }
                switch (currentState){
                    case INIT:
                        startX = currX;
                        startY = currY;
                        currentState = DRIVE_TRANSITION_STATE.DRIVE;
                        Log.d("INIT", "Initializing start position values");
                        break;
                    case DRIVE:
                        if (distanceTraveled(startX, startY, currX, currY) > distanceToTravel) {
                            Log.d("INIT", "Processing to finish");
                            distanceMade = true;
                            isEnabled = false;
                            if (isReportFinish) {
                                processDistanceSuccess(currX, currY);
                            } else {
                                finish();
                            }
                            return;
                        }
                        Log.d("INIT", "Continuing the drive using " + currentHeading);
                        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
                        break;
                }
            }
        }

        public void disable() {
            isEnabled = false;
        }
    }

    private class SquareTraveledListener implements SensorListener {

        private DRIVE_TRANSITION_STATE currentState;
        private boolean ifLeft;
        private float startX;
        private float startY;
        private int turnsMade;
        private boolean isEnabled;

        public SquareTraveledListener() {
            isEnabled = false;
        }

        public void initialize(boolean ifLeft) {
            this.ifLeft = ifLeft;
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
            this.turnsMade = 0;
            this.isEnabled = true;
        }

        private void resetPositionAndTurn(float newX, float newY) {
            Log.d("Reset", "Reseting from " + newX + " and " + newY);
            startX = newX;
            startY = newY;
            sphero.stop();
            if (ifLeft) {
                sphero.rotate(90.0f);
                currentHeading += 90.0f;
                currentHeading %= 360;
                sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
            } else {
                sphero.rotate(270.0f);
                currentHeading += 270.0f;
                currentHeading %= 360;
                sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
            }
        }

        @Override
        public void sensorUpdated(DeviceSensorsData deviceSensorsData) {
            if (isEnabled && deviceSensorsData != null && deviceSensorsData.getLocatorData() != null && deviceSensorsData.getLocatorData().getPosition() != null) {
                //currentHeading = deviceSensorsData.getAttitudeData().yaw;
                float currX = deviceSensorsData.getLocatorData().getPositionX();
                float currY = deviceSensorsData.getLocatorData().getPositionY();
                if (collisionDetected) {
                    processCollision(currX, currY);
                    return;
                }
                switch (currentState){
                    case INIT:
                        startX = currX;
                        startY = currY;
                        currentState = DRIVE_TRANSITION_STATE.DRIVE;
                        break;
                    case DRIVE:
                        if (turnsMade == 0) {
                            Log.d("First turn", "GO HERE");
                            if (distanceTraveled(startX, startY, currX, currY) > (SQUARE_LENGTH / 2.0f)) {
                                Log.d("First turn", "EVENTUALLY HERE");
                                resetPositionAndTurn(currX, currY);
                                turnsMade++;
                            }
                        }  else if (turnsMade == 4) {
                            if (distanceTraveled(startX, startY, currX, currY) > (SQUARE_LENGTH / 2.0f)) {
                                processSquareSuccess(currX, currY);
                            }
                        } else if (distanceTraveled(startX, startY, currX, currY) > SQUARE_LENGTH) {
                            resetPositionAndTurn(currX, currY);
                            turnsMade++;
                        }
                        break;
                }
            }
        }

        public void disable() {
            isEnabled = false;
        }
    }

    private class PointsTraveledListener implements SensorListener {

        private List<Coordinate> points;
        private DRIVE_TRANSITION_STATE currentState;
        private Coordinate currentPoint;
        private boolean isEnabled;

        public PointsTraveledListener() {
            isEnabled = false;
        }

        public void initialize(List<Coordinate> points) {
            this.points = new ArrayList<Coordinate>(points);
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
            isEnabled = true;
        }

        private float getDistance(Coordinate currentPoint, Coordinate newPoint) {
            double diffX = currentPoint.x - newPoint.x;
            double diffY = currentPoint.y - newPoint.y;
            return (float)(Math.pow(Math.pow(diffX, 2) + Math.pow(diffY, 2), 0.5));
        }

        private float getHeading(Coordinate currentPoint, Coordinate newPoint) {
            double diffX = currentPoint.x - newPoint.x;
            double diffY = currentPoint.y - newPoint.y;
            return (float) (Math.atan(diffY/diffX) * 180 / Math.PI);
        }

        @Override
        public void sensorUpdated(DeviceSensorsData deviceSensorsData) {
            if (isEnabled && deviceSensorsData != null && deviceSensorsData.getLocatorData() != null && deviceSensorsData.getLocatorData().getPosition() != null) {
                currentHeading = deviceSensorsData.getAttitudeData().yaw;
                while (currentHeading < 0) {
                    currentHeading += 360;
                }
                switch(currentState) {
                    case INIT:
                        float startX = deviceSensorsData.getLocatorData().getPositionX();
                        float startY = deviceSensorsData.getLocatorData().getPositionY();
                        currentPoint = new Coordinate(startX, startY);
                        currentState = DRIVE_TRANSITION_STATE.DRIVE;
                        distanceMade = true;
                        break;
                    case DRIVE:
                        float currX = deviceSensorsData.getLocatorData().getPositionX();
                        float currY = deviceSensorsData.getLocatorData().getPositionY();
                        if (collisionDetected) {
                            processCollision(currX, currY);
                            return;
                        }
                        if (distanceMade) {
                            if (points.isEmpty()) {
                                processPointsSuccess(currX, currY);
                            } else {
                                Coordinate newPoint = points.remove(0);
                                float heading = getHeading(currentPoint, newPoint);
                                float distance = getDistance(currentPoint, newPoint);
                                currentHeading = heading;
                                currentPoint = newPoint;
                                if (points.size() != 1) {
                                    processIntermediatePointSuccess(currX, currY);
                                }
                                drive(currentHeading, distance, false);
                            }
                            return;
                        }
                        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
                }
            }

        }

        public void disable() {
            isEnabled = false;
        }
    }

    private class CollisionReportListener implements CollisionListener {

        @Override
        public void collisionDetected(CollisionDetectedAsyncData collisionDetectedAsyncData) {
            collisionDetected = true;
            short impactX = collisionDetectedAsyncData.getImpactPower().x;
            short impactY = collisionDetectedAsyncData.getImpactPower().y;
            collisionAngle = (float) (Math.atan2(impactY, impactX) * 180.0f / Math.PI);
        }
    }

}

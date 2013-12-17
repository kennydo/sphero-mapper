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
    private static final float SQUARE_LENGTH = 1.0f;
    private static final float DEFAULT_DRIVE_SPEED = 0.5f;
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
        sphero.getCollisionControl().startDetection(40, 60, 40, 60, 40);
    }

    private synchronized void drive(float headingVariation, float distance, boolean isReportFinish) {
        sphero.stop();
        disableListeners();
        Log.d("DRIVING", "Starting to drive in " + headingVariation + " for " + distance);
        distanceTraveledListener.initialize(distance, isReportFinish);
        sphero.drive(currentHeading + headingVariation, DEFAULT_DRIVE_SPEED);
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
        sphero.drive(currentHeading, DEFAULT_DRIVE_SPEED);
    }

    public void makeLeftSquare() {
        squareTraveledListener.initialize(true);
        sphero.drive(currentHeading + 180.0f, DEFAULT_DRIVE_SPEED);
    }

    @Override
    public void makeLeftSquare(float collisionAngle) {
        sphero.rotate(collisionAngle);
        makeLeftSquare();
    }

    public void makeRightSquare() {
        squareTraveledListener.initialize(false);
        sphero.drive(currentHeading + 180.0f, DEFAULT_DRIVE_SPEED);
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
        Runner.getMapper().reportEvent(collisionEvent);
        collisionDetected = false;
        finish();
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

    private synchronized void processEvent(float x, float y, MappingEvent.Type event) {
        MappingEvent collisionEvent = new MappingEvent(event, x, y);
        Runner.getMapper().reportEvent(collisionEvent);
        finish();
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
            if (isEnabled) {
                currentHeading = deviceSensorsData.getAttitudeData().yaw;
                while (currentHeading < 0) {
                    currentHeading += 360;
                }
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
        }

        private void resetPositionAndTurn(float newX, float newY) {
            startX = newX;
            startY = newY;
            if (ifLeft) {
                sphero.drive(currentHeading + 270.0f, DEFAULT_DRIVE_SPEED);
            } else {
                sphero.drive(currentHeading + 90.0f, DEFAULT_DRIVE_SPEED);
            }
        }

        @Override
        public void sensorUpdated(DeviceSensorsData deviceSensorsData) {
            if (isEnabled) {
                currentHeading = deviceSensorsData.getAttitudeData().yaw;
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
                            if (distanceTraveled(startX, startY, currX, currY) > SQUARE_LENGTH / 2) {
                                resetPositionAndTurn(currX, currY);
                                turnsMade++;
                            }
                        }  else if (turnsMade == 4) {
                            if (distanceTraveled(startX, startY, currX, currY) > SQUARE_LENGTH / 2) {
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

        public PointsTraveledListener(List<Coordinate> points) {
            this.points = new ArrayList<Coordinate>(points);
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
        }

        public PointsTraveledListener() {
            isEnabled = false;
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
            if (isEnabled) {
                currentHeading = deviceSensorsData.getAttitudeData().yaw;
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
                                currentPoint = newPoint;
                                drive(heading, distance, false);
                            }
                        }
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
            if (impactX == 0) {
                if (impactY > 0) {
                    collisionAngle = 90;
                } else {
                    collisionAngle = 270;
                }
            } else {
                collisionAngle = (float) Math.atan((impactY) * 1.0 / (impactX * 1.0));
            }
        }
    }

}

package edu.berkeley.spheromapper;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.mapping.Commander;
import edu.berkeley.mapping.MappingEvent;
import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.sensor.LocatorData;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.LocatorListener;
import orbotix.sphero.Sphero;

/**
 * Created by Brandon on 11/26/13.
 */
public class SpheroCommander implements Commander{

    private Sphero sphero;
    private static final float SQUARE_LENGTH = 2.0f;
    private boolean collisionDetected;
    private boolean distanceMade;

    private enum DRIVE_TRANSITION_STATE {
        INIT, DRIVE
    }

    public SpheroCommander(Sphero sphero) {
        this.sphero = sphero;
        collisionDetected = false;
        sphero.getCollisionControl().addCollisionListener(new CollisionReportListener());
    }

    private void drive(float headingVariation, float distance, boolean isReportFinish) {
        sphero.getSensorControl().addLocatorListener(new DistanceTraveledListener(distance, isReportFinish));
        sphero.drive(headingVariation, 1.0f);
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
        sphero.getSensorControl().addLocatorListener(new PointsTraveledListener(points));
        sphero.drive(0f, 1.0f);
    }

    @Override
    public void makeLeftSquare() {
        sphero.getSensorControl().addLocatorListener(new SquareTraveledListener(true));
        sphero.drive(180.0f, 1.0f);
    }

    @Override
    public void makeLeftSquare(float collisionAngle) {
        sphero.rotate(collisionAngle);
        makeLeftSquare();
    }

    @Override
    public void makeRightSquare() {
        sphero.getSensorControl().addLocatorListener(new SquareTraveledListener(false));
        sphero.drive(180.0f, 1.0f);
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

    public static float distanceTraveled(float startX, float startY, float endX, float endY) {
        return (float) Math.pow(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2), 0.5);
    }

    private void processCollision(LocatorListener listener, float x, float y) {
        processEvent(listener, x, y, MappingEvent.Type.COLLISION);
        collisionDetected = false;
    }

    private void processSquareSuccess(LocatorListener listener, float x, float y) {
        processEvent(listener, x, y, MappingEvent.Type.SQUARE_COMPLETED);
    }

    private void processDistanceSuccess(LocatorListener listener, float x, float y) {
        processEvent(listener, x, y, MappingEvent.Type.DISTANCE_REACHED);
    }

    private void processPointsSuccess(LocatorListener listener, float x, float y) {
        processEvent(listener, x, y, MappingEvent.Type.POINTS_COMPLETED);
    }

    private void processEvent(LocatorListener listener, float x, float y, MappingEvent.Type event) {
        MappingEvent collisionEvent = new MappingEvent(event, x, y);
        Runner.getMapper().reportEvent(collisionEvent);
        finish(listener);
    }

    private void finish(LocatorListener listener) {
        sphero.stop();
        sphero.getSensorControl().removeLocatorListener(listener);
    }

    private class DistanceTraveledListener implements LocatorListener {

        private float distanceToTravel;
        private DRIVE_TRANSITION_STATE currentState;
        private float startX;
        private float startY;
        private boolean isReportFinish;

        public DistanceTraveledListener(float distanceToTravel, boolean isReportFinish) {
            this.distanceToTravel = distanceToTravel;
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
            this.isReportFinish = isReportFinish;
            distanceMade = false;
        }

        @Override
        public void onLocatorChanged(LocatorData locatorData) {
            float currX = locatorData.getPositionX();
            float currY = locatorData.getPositionY();
            if (collisionDetected) {
                processCollision(this, currX, currY);
                return;
            }
            switch (currentState){
                case INIT:
                    startX = currX;
                    startY = currY;
                    currentState = DRIVE_TRANSITION_STATE.DRIVE;
                    break;
                case DRIVE:
                    if (distanceTraveled(startX, startY, currX, currY) > distanceToTravel) {
                        distanceMade = true;
                        if (isReportFinish) {
                            processDistanceSuccess(this, currX, currY);
                        } else {
                            finish(this);
                        }
                    }
                    break;
            }
        }
    }

    private class SquareTraveledListener implements LocatorListener {

        private DRIVE_TRANSITION_STATE currentState;
        private boolean ifLeft;
        private float startX;
        private float startY;
        private int turnsMade;

        public SquareTraveledListener(boolean ifLeft) {
            this.ifLeft = ifLeft;
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
            this.turnsMade = 0;
        }

        @Override
        public void onLocatorChanged(LocatorData locatorData) {
            float currX = locatorData.getPositionX();
            float currY = locatorData.getPositionY();
            if (collisionDetected) {
                processCollision(this, currX, currY);
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
                            processSquareSuccess(this, currX, currY);
                        }
                    } else if (distanceTraveled(startX, startY, currX, currY) > SQUARE_LENGTH) {
                        resetPositionAndTurn(currX, currY);
                        turnsMade++;
                    }
                    break;
            }
        }

        private void resetPositionAndTurn(float newX, float newY) {
            startX = newX;
            startY = newY;
            if (ifLeft) {
                sphero.drive(270.0f, 1.0f);
            } else {
                sphero.drive(90.0f, 1.0f);
            }
        }
    }

    private class PointsTraveledListener implements LocatorListener {

        private List<Coordinate> points;
        private DRIVE_TRANSITION_STATE currentState;
        private Coordinate currentPoint;

        public PointsTraveledListener(List<Coordinate> points) {
            this.points = new ArrayList<Coordinate>(points);
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
        }

        @Override
        public void onLocatorChanged(LocatorData locatorData) {
            switch(currentState) {
                case INIT:
                    float startX = locatorData.getPositionX();
                    float startY = locatorData.getPositionY();
                    currentPoint = new Coordinate(startX, startY);
                    currentState = DRIVE_TRANSITION_STATE.DRIVE;
                    distanceMade = true;
                    break;
                case DRIVE:
                    float currX = locatorData.getPositionX();
                    float currY = locatorData.getPositionY();
                    if (collisionDetected) {
                        processCollision(this, currX, currY);
                        return;
                    }
                    if (distanceMade) {
                        if (points.isEmpty()) {
                            processPointsSuccess(this, currX, currY);
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
    }

    private class CollisionReportListener implements CollisionListener {

        @Override
        public void collisionDetected(CollisionDetectedAsyncData collisionDetectedAsyncData) {
            collisionDetected = true;
        }
    }

}

package edu.berkeley.spheromapper;

import edu.berkeley.mapping.Commander;
import edu.berkeley.mapping.MappingEvent;
import edu.berkeley.mapping.Runner;
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

    private enum DRIVE_TRANSITION_STATE {
        INIT, DRIVE
    }

    public SpheroCommander(Sphero sphero) {
        this.sphero = sphero;
        collisionDetected = false;
    }

    @Override
    public void drive(float headingVariation, float distance) {
        sphero.getSensorControl().addLocatorListener(new DistanceTraveledListener(distance));
        sphero.drive(headingVariation, 1.0f);
    }

    @Override
    public void drive(float headingVariation) {
        drive(headingVariation, Float.MAX_VALUE);
    }

    @Override
    public void makeLeftSquare() {
        sphero.getSensorControl().addLocatorListener(new SquareTraveledListener(true));
        sphero.drive(180.0f, 1.0f);
    }

    @Override
    public void makeRightSquare() {
        sphero.getSensorControl().addLocatorListener(new SquareTraveledListener(false));
        sphero.drive(180.0f, 1.0f);
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

    private void processEvent(LocatorListener listener, float x, float y, MappingEvent.Type event) {
        MappingEvent collisionEvent = new MappingEvent(event, x, y);
        Runner.getMapper().reportEvent(collisionEvent);
        sphero.stop();
        sphero.getSensorControl().removeLocatorListener(listener);
    }

    private class DistanceTraveledListener implements LocatorListener {

        private float distanceToTravel;
        private DRIVE_TRANSITION_STATE currentState;
        private float startX;
        private float startY;

        public DistanceTraveledListener(float distanceToTravel) {
            this.distanceToTravel = distanceToTravel;
            this.currentState = DRIVE_TRANSITION_STATE.INIT;
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
                        processDistanceSuccess(this, currX, currY);
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
                    if (distanceTraveled(startX, startY, currX, currY) > SQUARE_LENGTH) {
                        if (turnsMade == 2) {
                            processSquareSuccess(this, currX, currY);
                        } else {
                            resetPositionAndTurn(currX, currY);
                            turnsMade++;
                        }
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

    private class CollisionReportListener implements CollisionListener {

        @Override
        public void collisionDetected(CollisionDetectedAsyncData collisionDetectedAsyncData) {
            collisionDetected = true;
        }
    }
}

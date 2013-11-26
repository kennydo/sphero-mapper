package edu.berkeley.spheromapper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import orbotix.robot.sensor.LocatorData;

/**
 * Created by kedo on 11/25/13.
 */
public class MapSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private MapThread thread;
    private List<LocatorData> collisions;
    private Float xMax, xMin, yMax, yMin;
    private Float xAbsMax, yAbsMax;

    private class MapThread extends Thread{
        private SurfaceHolder mSurfaceHolder;
        private Context mContext;
        private Paint collisionPaint;
        private final Object mRunLock = new Object();
        private boolean mRun;

        public MapThread(SurfaceHolder surfaceHolder, Context context) {
            mSurfaceHolder = surfaceHolder;
            mContext = context;

            collisionPaint = new Paint();
            collisionPaint.setColor(Color.GREEN);
            collisionPaint.setAntiAlias(true);
            collisionPaint.setStyle(Paint.Style.FILL);
            mRun = true;
        }

        @Override
        public void run() {
            while(mRun){
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized (mRunLock) {
                            if (mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        private void doDraw(Canvas c){
            c.drawColor(Color.DKGRAY);

            float centerX = getWidth() / 2;
            float centerY = getHeight() / 2;
            //Log.i("MapSurfaceView", "centerX=" + centerX + ", centerY=" + centerY);
            //Log.i("MapSurfaceView", collisions.toString());

            float x, y;
            float drawX, drawY;
            float xDiff = xMax - xMin;
            float yDiff = yMax - yMin;
            for(LocatorData location : collisions){
                x = location.getPositionX();
                y = location.getPositionY();

                drawX = centerX + ((x - xDiff) * (centerX / xAbsMax));
                drawY = centerY + ((y - yDiff) * (centerY / yAbsMax));

                c.drawCircle(drawX, drawY, 10, collisionPaint);
                //Log.i("MapSurfaceView", "drawing collision at x=" + drawX + ", y=" + drawY);
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized (mRunLock) {
                mRun = b;
            }
        }

    }

    public MapSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new MapThread(holder, context);

        collisions = new ArrayList<LocatorData>();
    }

    public void addCollision(LocatorData location){
        synchronized(collisions){
            collisions.add(0, location);
            float x, y;
            x = location.getPositionX();
            y = location.getPositionY();
            if(xMin == null){
                xMin = x;
            }
            if(xMax == null){
                xMax = x;
            }
            if(yMin == null){
                yMin = y;
            }
            if(yMax == null){
                yMax = y;
            }

            if(x < xMin){
                xMin = x;
            } else if (x > xMax){
                xMax = x;
            }

            if(y < yMin){
                yMin = y;
            } else if (y > yMax){
                yMax = y;
            }

            xAbsMax = Math.max(xMax, Math.abs(xMin));
            yAbsMax = Math.max(yMax, Math.abs(yMin));
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}
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

/**
 * Created by kedo on 11/25/13.
 */
public class MapSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private MapThread thread;

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
            c.drawColor(Color.BLUE);
            c.drawCircle(0, 0, 10, collisionPaint);
            c.drawCircle(30, 0, 10, collisionPaint);
            c.drawCircle(0, 60, 10, collisionPaint);
            c.drawCircle((float) (getWidth() / 2.0), (float) (getHeight() / 2.0), 100, collisionPaint);
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
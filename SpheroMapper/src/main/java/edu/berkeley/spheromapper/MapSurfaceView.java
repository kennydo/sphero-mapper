package edu.berkeley.spheromapper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

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

            Geometry freeGeometry = Runner.getMapper().getFreeGeometry();
/*
            String wkt = "MULTIPOLYGON (((-120 180, 152 180, 152 165, -120 165, -120 180)), \n" +
                    "  ((-130 190, -110 190, -110 -120, -130 -120, -130 190)), \n" +
                    "  ((-140 -110, 170 -110, 170 -130, -140 -130, -140 -110)), \n" +
                    "  ((176 -155, 140 -155, 140 200, 176 200, 176 -155)))";
            WKTReader wKTReader = new WKTReader();
            try{
                freeGeometry = wKTReader.read(wkt);
            } catch (ParseException e){
                Log.d("MapSurfaceView", "parsing error");
                return;
            }
            */

            Envelope freeEnvelope = freeGeometry.getEnvelopeInternal();
            Coordinate freeCenter = freeEnvelope.centre();

            CoordinateConverter converter = new CoordinateConverter(freeEnvelope);

            for(int i = freeGeometry.getNumGeometries() - 1; i >= 0; i--){
                Geometry geometry = freeGeometry.getGeometryN(i);

                Coordinate[] coordinates = geometry.getCoordinates();
                if(coordinates.length > 1){
                    Coordinate coordinate = coordinates[0];
                    Path path = new Path();
                    float x, y;

                    path.moveTo((float) converter.convertX(coordinate.x),
                            (float) converter.convertY(coordinate.y));
                    for(int j=1; j< coordinates.length; j++){
                        coordinate = coordinates[j];
                        path.lineTo((float) converter.convertX(coordinate.x),
                                (float) converter.convertY(coordinate.y));
                    }
                    coordinate = coordinates[0];
                    path.moveTo((float) converter.convertX(coordinate.x),
                            (float) converter.convertY(coordinate.y));

                    Paint paint = new Paint();
                    paint.setColor(Color.YELLOW);
                    paint.setStyle(Paint.Style.FILL);
                    c.drawPath(path, paint);
                }
            }


        }

        private class CoordinateConverter {
            private double canvasWidth, canvasHeight;
            private double pointCenterX, pointCenterY;
            private double minX, maxX, minY, maxY;
            private double sizeX, sizeY;

            public CoordinateConverter(Envelope envelope){
                this(envelope.getMinX(),
                        envelope.getMaxX(),
                        envelope.getMinY(),
                        envelope.getMaxY());
            }

            public CoordinateConverter(double minX, double maxX, double minY, double maxY){
                canvasWidth = getWidth();
                canvasHeight = getHeight();

                this.minX = minX;
                this.maxY = maxX;
                this.minY = minY;
                this.maxY = maxY;

                pointCenterX = (minX + maxX) * 0.5;
                pointCenterY = (minY + maxY) * 0.5;

                //Log.d("CoordinateConverter", "Center of envelope is (" + pointCenterX + ", " + pointCenterY + ")");

                sizeX = Math.abs(maxX - minX) * 1.1;
                sizeY = Math.abs(maxY - minY) * 1.1;

                //Log.d("CoordinateConverter", "Size is " + sizeX + ", " + sizeY);
            }

            public double convertX(double x){
                double newX = ((x - pointCenterX) / sizeX) * canvasWidth + (canvasWidth / 2);
                //Log.d("CoordinateConverter", "Converted x=" + x + " to " + newX);
                return newX;
            }
            public double convertY(double y){
                double newY = ((y - pointCenterY) / sizeY) * canvasHeight + (canvasHeight / 2);
                //Log.d("CoordinateConverter", "Converted y=" + y + " to " + newY);
                return newY;
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
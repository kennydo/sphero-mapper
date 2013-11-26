package edu.berkeley.spheromapper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.sphero.Sphero;

/**
 * Created by kedo on 11/25/13.
 */
public class ViewMapFragment extends Fragment implements SpheroListenerFragment {

    private View rootView;
    private SurfaceView surface;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_map, container, false);
        return rootView;
    }

    @Override
    public void onSpheroSensorsUpdate(DeviceSensorsData sensorsData) {

    }

    @Override
    public void onSpheroCollision(CollisionDetectedAsyncData collisionData, DeviceSensorsData sensorsData) {

    }

    @Override
    public void setSphero(Sphero sphero) {

    }
}
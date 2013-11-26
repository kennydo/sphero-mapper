package edu.berkeley.spheromapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.robot.sensor.LocatorSensor;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.SensorFlag;


/**
 * Created by kedo on 11/7/13.
 */
public class ManualDriveFragment extends ListFragment implements SpheroListenerFragment {
    private View rootView;
    private Sphero mSphero;

    private boolean logNextLocationPoll;

    private int collisionBufferSizeLimit = 7;
    private ArrayList<String> collisionLocations;

    private ArrayAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_manual_drive, container, false);
        if(savedInstanceState != null && savedInstanceState.containsKey("collisionLocations")){
            collisionLocations = savedInstanceState.getStringArrayList("collisionLocations");
        } else {
            collisionLocations = new ArrayList<String>();
        }
        logNextLocationPoll = false;

        hasSphero(); // this sets mSphero

        DirectionalTouchListener directionalListener = new DirectionalTouchListener();

        Button[] buttons = {
                (Button) rootView.findViewById(R.id.up_button),
                (Button) rootView.findViewById(R.id.left_button),
                (Button) rootView.findViewById(R.id.down_button),
                (Button) rootView.findViewById(R.id.right_button),
        };
        for(Button button : buttons){
            button.setOnTouchListener(directionalListener);
        }

        ListView collisionListView = (ListView) rootView.findViewById(android.R.id.list);

        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, collisionLocations);
        collisionListView.setAdapter(mAdapter);

        ((Button) rootView.findViewById(R.id.clear_collisions_button)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                collisionLocations.clear();
                mAdapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("collisionLocations", collisionLocations);
    }

    private boolean hasSphero(){
        if(mSphero != null){
            return true;
        }

        List<Sphero> robots = RobotProvider.getDefaultProvider().getRobots();
        Log.d("ManualDrive", "Got these robots: " + robots.toString());
        if(robots.size() > 0){
            Sphero candidateSphero = robots.get(0);
            if(!candidateSphero.isConnected()){
                Log.e("ManualDrive", "Had to manually set connected to True");
                candidateSphero.setConnected(true);
            }
            mSphero = candidateSphero;
        } else {
            Log.d("ManualDrive", "No sphero connected");
            Toast.makeText(getActivity(), "No Sphero Connected", Toast.LENGTH_LONG);
        }

        if(mSphero != null){
            mSphero.getSensorControl().setRate(5);
            mSphero.getSensorControl().addSensorListener(new SensorListener() {
                @Override
                public void sensorUpdated(DeviceSensorsData deviceSensorsData) {
                    LocatorData locatorData = deviceSensorsData.getLocatorData();
                    float x = locatorData.getPositionX();
                    float y = locatorData.getPositionY();

                    String coordinates = "(" + x + ", " + y + ")";

                    ((TextView) rootView.findViewById(R.id.location_text)).setText(coordinates);

                    if(logNextLocationPoll){
                        Log.d("ManualDrive", "Collision logged at: " + coordinates);
                        collisionLocations.add(0, coordinates); // we want the most recent at the top of the list
                        for(int i=collisionLocations.size() - 1; i > collisionBufferSizeLimit; i--){
                            collisionLocations.remove(i);
                        }
                        mAdapter.notifyDataSetChanged();
                        logNextLocationPoll = false;
                    }
                }
            }, SensorFlag.ACCELEROMETER_NORMALIZED, SensorFlag.ATTITUDE, SensorFlag.LOCATOR);

            mSphero.getCollisionControl().startDetection(40, 60, 40, 60, 40);
            mSphero.getCollisionControl().addCollisionListener(new CollisionListener() {
                @Override
                public void collisionDetected(CollisionDetectedAsyncData collisionDetectedAsyncData) {
                    Log.i("ManualDrive", "Collision detected!");
                    logNextLocationPoll = true;
                }
            });

            return true;
        }


        return false;
    }

    private class DirectionalTouchListener implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(!hasSphero()){ return false; }

            if (event.getAction() == MotionEvent.ACTION_DOWN){
                float heading;
                switch(v.getId()){
                    case R.id.up_button:
                        heading = 0f;
                        break;
                    case R.id.left_button:
                        heading = 270f;
                        break;
                    case R.id.right_button:
                        heading = 90f;
                        break;
                    case R.id.down_button:
                        heading = 180f;
                        break;
                    default:
                        heading = 0f;
                }
                float speed = 0.6f;

                mSphero.drive(heading, speed);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP){
                mSphero.stop();
                return true;
            }

            return false;
        }
    }
}
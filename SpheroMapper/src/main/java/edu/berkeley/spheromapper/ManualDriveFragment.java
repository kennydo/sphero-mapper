package edu.berkeley.spheromapper;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.sphero.Sphero;


/**
 * Created by kedo on 11/7/13.
 */
public class ManualDriveFragment extends ListFragment implements SpheroListenerFragment {
    private View rootView;
    private Sphero mSphero;

    private final int COLLISION_HISTORY_SIZE = 7;
    private ArrayList<String> collisionLocations;

    private float previousX = 0;
    private float previousY = 0;

    private ArrayAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_manual_drive, container, false);
        if(savedInstanceState != null && savedInstanceState.containsKey("collisionLocations")){
            collisionLocations = savedInstanceState.getStringArrayList("collisionLocations");
        } else {
            collisionLocations = new ArrayList<String>();
        }

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

        populateCollisionHistory(((CollisionLocationHistoryProvider) getActivity()).getCollisionLocations());

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("collisionLocations", collisionLocations);
    }

    private class DirectionalTouchListener implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(mSphero == null){ return false; }

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

    public void onSpheroSensorsUpdate(DeviceSensorsData sensorsData){
        LocatorData locatorData = sensorsData.getLocatorData();

        String coordinates = locatorDataToString(locatorData);
        ((TextView) rootView.findViewById(R.id.location_text)).setText(coordinates);
    }

    public void onSpheroCollision(CollisionDetectedAsyncData collisionData, DeviceSensorsData sensorsData){
        LocatorData locatorData = sensorsData.getLocatorData();
        String coordinates = locatorDataToString(locatorData);
        Log.d("ManualDrive", "Collision logged at: " + coordinates);
        collisionLocations.add(0, coordinates); // we want the most recent at the top of the list
        for(int i=collisionLocations.size() - 1; i > COLLISION_HISTORY_SIZE; i--){
            collisionLocations.remove(i);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void setSphero(Sphero sphero){
        mSphero = sphero;
    }

    private String locatorDataToString(LocatorData locatorData){
        float x, y;
        if(locatorData == null){
            x = previousX;
            y = previousY;
        } else {
            x = locatorData.getPositionX();
            y = locatorData.getPositionY();
        }

        previousX = x;
        previousY = y;
        return "(" + x + ", " + y + ")";
    }

    public void populateCollisionHistory(List<LocatorData> locations){
        if(locations != null){
            collisionLocations.clear();
            for(int i=0; (i<COLLISION_HISTORY_SIZE) && (i < locations.size()); i++){
                collisionLocations.add(i, locatorDataToString(locations.get(i)));
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
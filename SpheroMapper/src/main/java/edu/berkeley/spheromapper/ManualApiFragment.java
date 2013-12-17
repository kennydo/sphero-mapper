package edu.berkeley.spheromapper;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.mapping.Commander;
import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.sphero.Sphero;

/**
 * Created by kedo on 12/13/13.
 */
public class ManualApiFragment extends ListFragment implements SpheroListenerFragment {
    private View rootView;
    private Sphero mSphero;

    private final int COLLISION_HISTORY_SIZE = 7;
    private ArrayList<String> collisionLocations;

    private float previousX = 0;
    private float previousY = 0;

    private ArrayAdapter mAdapter;

    private Commander commander;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_manual_api, container, false);
        if(savedInstanceState != null && savedInstanceState.containsKey("collisionLocations")){
            collisionLocations = savedInstanceState.getStringArrayList("collisionLocations");
        } else {
            collisionLocations = new ArrayList<String>();
        }

        Button leftSquareButton, rightSquareButton, stopButton, driveButton;

        leftSquareButton = (Button) rootView.findViewById(R.id.left_square_button);
        rightSquareButton = (Button) rootView.findViewById(R.id.right_square_button);
        stopButton = (Button) rootView.findViewById(R.id.stop_button);
        driveButton = (Button) rootView.findViewById(R.id.drive_button);

        leftSquareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("ManualApi", "Left Square button pressed");

                EditText editAngle = (EditText) rootView.findViewById(R.id.edit_angle);
                Float angle = Float.valueOf(editAngle.getText().toString());
                Log.d("ManualApi", "Left square button pressed with angle=" + angle);
                if(hasCommander()){
                    // call the SpheroCommander here
                    commander.makeLeftSquare(angle);
                }
            }
        });
        rightSquareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("ManualApi", "Right Square button pressed");

                EditText editAngle = (EditText) rootView.findViewById(R.id.edit_angle);
                Float angle = Float.valueOf(editAngle.getText().toString());
                Log.d("ManualApi", "Right square button pressed with angle=" + angle);
                if(hasCommander()){
                    // call the SpheroCommander here
                    commander.makeRightSquare(angle);
                }
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("ManualApi", "Stop button pressed");
                if(hasCommander()){
                    // call the SpheroCommander here
                    commander.stop();
                }
            }
        });
        driveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("ManualApi", "Drive button pressed, parsing data");
                // call the SpheroCommander here

                EditText editHeading = (EditText) rootView.findViewById(R.id.edit_heading);
                EditText editDistance = (EditText) rootView.findViewById(R.id.edit_distance);

                Float heading = Float.valueOf(editHeading.getText().toString());
                Float distance = Float.valueOf(editDistance.getText().toString());
                Log.d("ManualApi", "Drive button pressed with heading=" + heading + ", distance=" + distance);

                if(hasCommander()){
                    // call the SpheroCommander here
                    commander.drive(heading, distance);
                }
            }
        });

        ListView collisionListView = (ListView) rootView.findViewById(android.R.id.list);

        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, collisionLocations);
        collisionListView.setAdapter(mAdapter);

        ((Button) rootView.findViewById(R.id.clear_collisions_button)).setOnClickListener(new View.OnClickListener() {
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
    public void onSpheroSensorsUpdate(DeviceSensorsData sensorsData) {
        LocatorData locatorData = sensorsData.getLocatorData();

        String coordinates = locatorDataToString(locatorData);
        TextView locationText = (TextView) rootView.findViewById(R.id.location_text);
        if(locationText != null){
         locationText.setText(coordinates);
        } else {
            Log.d("ManualApi", "Could not access the location_text element, so couldn't update coordinates to " + coordinates);
        }
    }

    @Override
    public void onSpheroCollision(CollisionDetectedAsyncData collisionData, DeviceSensorsData sensorsData) {
        LocatorData locatorData = sensorsData.getLocatorData();
        String coordinates = locatorDataToString(locatorData);
        Log.d("ManualApi", "Collision logged at: " + coordinates);
        collisionLocations.add(0, coordinates); // we want the most recent at the top of the list
        for(int i=collisionLocations.size() - 1; i > COLLISION_HISTORY_SIZE; i--){
            collisionLocations.remove(i);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setSphero(Sphero sphero){
        mSphero = sphero;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("collisionLocations", collisionLocations);
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

    private boolean hasCommander(){
        if(commander != null){
            Log.d("ManualApi", "commander wasn't null, so hasCommander returning true");
            return true;
        }
        MainActivity mainActivity = (MainActivity) getActivity();

        commander = Runner.getCommander();
        if(commander != null){
            Log.d("ManualApi", "Successfully got commander from Runner");
            return true;
        }
        Log.d("ManualApi", "Failed to get commander from Runner");
        return false;
    }
}
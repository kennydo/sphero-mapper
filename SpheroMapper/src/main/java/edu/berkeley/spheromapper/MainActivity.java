package edu.berkeley.spheromapper;

import android.bluetooth.BluetoothAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import orbotix.robot.base.*;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import orbotix.view.connection.SpheroConnectionView;

public class MainActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, CollisionLocationHistoryProvider {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    // the maximum number of collisions we want to remember
    private static final int MAX_NUM_COLLISION_HISTORY = 100;

    private SpheroConnectionView mSpheroConnectionView;
    private Sphero mSphero;
    private SpheroListenerFragment activeFragment;
    private boolean logNextSensorsData = false;
    private CollisionDetectedAsyncData previousCollisionDetectedAsyncData;

    // A list of the locations of where collisions happened.
    // The newest collision locations are at the front of the list (index 0),
    // and the oldest collision is at index (MAX_NUM_COLLISION_HISTORY - 1).
    private List<LocatorData> collisionLocationHistory = new ArrayList<LocatorData>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(
                        actionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[]{
                                getString(R.string.title_manual_drive),
                                getString(R.string.title_section3),
                        }),
                this);
        actionBar.hide();

        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);

        Log.i("Connection", "Robots: " + RobotProvider.getDefaultProvider().getRobots().toString());

        // Set the connection event listener
        mSpheroConnectionView.addConnectionListener(new ConnectionListener() {
            // Invoked when Sphero is connected & ready for commands
            @Override
            public void onConnected(Robot sphero){
                Log.i("Connection", "onConnected fired");
                onSpheroConnected(sphero);
            }

            // Invoked when Sphero fails to complete a connect request
            @Override
            public void onConnectionFailed(Robot sphero){
                Log.i("Connection", "onConnectionFailed fired");
            }

            // Invoked when Sphero disconnects for any reason
            @Override
            public void onDisconnected(Robot sphero){
                Log.i("Connection", "onDisconnected fired");

                mSpheroConnectionView.startDiscovery();
            }
        });

        mSpheroConnectionView.addDiscoveryListener(new DiscoveryListener() {
            @Override
            public void onBluetoothDisabled() {
                Log.d("Connection", "onBluetoothDisabled fired");
                // See UISample Sample on how to show BT settings screen, for now just notify user
                Toast.makeText(MainActivity.this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void discoveryComplete(List<Sphero> spheros) {
                Log.d("Connection", "discoveryComplete fired");
            }

            @Override
            public void onFound(List<Sphero> spheros) {
                Log.d("Connection", "onFound fired");
            }
        });
    }

    /**
     * Called when the user comes back to this app
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(mSpheroConnectionView != null) {
            mSpheroConnectionView.startDiscovery();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        if (mSphero != null) {
            mSphero.disconnect(); // Disconnect Robot properly
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getSupportActionBar().getSelectedNavigationIndex());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        if(mSpheroConnectionView.getVisibility() != View.INVISIBLE){
            Log.e("MainActivity", "Tried to call onNavigationItemSelected when mSpheroConnectionView is not invisible");
            return false;
        }

        Fragment fragment;
        switch(position){
            case 0: // manual drive
                fragment = new ManualDriveFragment();
                break;

            default:
                fragment = new ManualDriveFragment();
        }
        ((SpheroListenerFragment) fragment).setSphero(mSphero);
       getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        activeFragment = (SpheroListenerFragment) fragment;
        return true;
    }


    @Override
    public void onStop(){
        super.onStop();

        // Disconnect from the robot.
        RobotProvider.getDefaultProvider().removeAllControls();
    }

    private void onSpheroConnected(Robot sphero){
        // It looks like the SDK automatically pops up a Toast, so we don't need to make our own.
        //Toast.makeText(activity, "Successfully connected to Sphero", Toast.LENGTH_LONG).show();

        // Skip this next step if you want the user to be able to connect multiple Spheros
        mSpheroConnectionView.setVisibility(View.INVISIBLE);
        Log.d("Connection", "set visibility of mSpheroConnectionView to INVISIBLE");

        getSupportActionBar().show();

        mSphero = (Sphero) sphero;
        mSphero.getSensorControl().setRate(5);
        mSphero.getSensorControl().addSensorListener(new SensorListener() {
            @Override
            public void sensorUpdated(DeviceSensorsData deviceSensorsData) {
                if(activeFragment != null){
                    activeFragment.onSpheroSensorsUpdate(deviceSensorsData);
                    if(logNextSensorsData){
                        Log.i("MainActivity", "Called fragment's collision callback");
                        activeFragment.onSpheroCollision(previousCollisionDetectedAsyncData, deviceSensorsData);
                        logNextSensorsData = false;

                        collisionLocationHistory.add(0, deviceSensorsData.getLocatorData());
                        for(int i=collisionLocationHistory.size(); i >= MAX_NUM_COLLISION_HISTORY; i--){
                            collisionLocationHistory.remove(i);
                        }
                        Log.e("MainActivity", collisionLocationHistory.toString());
                    }
                }
            }
        }, SensorFlag.ACCELEROMETER_NORMALIZED, SensorFlag.ATTITUDE, SensorFlag.LOCATOR);

        mSphero.getCollisionControl().startDetection(40, 60, 40, 60, 40);
        mSphero.getCollisionControl().addCollisionListener(new CollisionListener() {
            @Override
            public void collisionDetected(CollisionDetectedAsyncData collisionDetectedAsyncData) {
                Log.i("MainActivity", "Collision detected!");
                if(activeFragment != null){
                    previousCollisionDetectedAsyncData = collisionDetectedAsyncData;
                    logNextSensorsData = true;
                }
            }
        });
    }

    @Override
    public List<LocatorData> getCollisionLocations() {
        return collisionLocationHistory;
    }
}

package edu.berkeley.spheromapper;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.Sphero;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;

/**
 * Created by kedo on 11/11/13.
 */
public class ConnectionFragment extends Fragment {
    /**
     * The Sphero Connection View
     */
    SpheroConnectionView mSpheroConnectionView;
    private Activity activity;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_connection, container, false);

        activity = getActivity();
        mSpheroConnectionView = (SpheroConnectionView) rootView.findViewById(R.id.sphero_connection_view);

        Log.i("ConnectionFragment", "Robots: " + RobotProvider.getDefaultProvider().getRobots().toString());

        // Set the connection event listener
        mSpheroConnectionView.addConnectionListener(new ConnectionListener() {
            // Invoked when Sphero is connected & ready for commands
            @Override
            public void onConnected(Robot sphero){
                Log.i("ConnectionFragment", "onConnected fired");

                Toast.makeText(activity, "Successfully connected to Sphero", Toast.LENGTH_LONG).show();

                // Skip this next step if you want the user to be able to connect multiple Spheros
                //mSpheroConnectionView.setVisibility(View.GONE);
                //Log.d("Sphero", "set visibility of mSpheroConnectionView to GNONE");
            }

            // Invoked when Sphero fails to complete a connect request
            @Override
            public void onConnectionFailed(Robot sphero){
                Log.i("ConnectionFragment", "onConnectionFailed fired");
            }

            // Invoked when Sphero disconnects for any reason
            @Override
            public void onDisconnected(Robot sphero){
                Log.i("ConnectionFragment", "onDisconnected fired");

                //mSpheroConnectionView.startDiscovery();
            }
        });

        mSpheroConnectionView.addDiscoveryListener(new DiscoveryListener() {
            @Override
            public void onBluetoothDisabled() {
                Log.d("ConnectionFragment", "onBluetoothDisabled fired");
                // See UISample Sample on how to show BT settings screen, for now just notify user
                Toast.makeText(activity, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void discoveryComplete(List<Sphero> spheros) {
                Log.d("ConnectionFragment", "discoveryComplete fired");
            }

            @Override
            public void onFound(List<Sphero> spheros) {
                Log.d("ConnectionFragment", "onFound fired");
            }
        });

        mSpheroConnectionView.startDiscovery();

        return rootView;
    }

    @Override
    public void onStop(){
        if(mSpheroConnectionView != null){
            mSpheroConnectionView.clearListeners();
        }
        super.onStop();
    }
}
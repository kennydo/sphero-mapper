package edu.berkeley.spheromapper;

import java.util.List;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import orbotix.sphero.Sphero;
import orbotix.robot.base.RobotProvider;


/**
 * Created by kedo on 11/7/13.
 */
public class ManualDriveFragment extends Fragment {
    private View rootView;
    private Sphero mSphero;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_manual_drive, container, false);

        hasSphero(); // this sets mSphero

        OnClickListener directionalistener = new DirectionalClickListener();
        OnClickListener stopListener = new StopClickListener();

        Button[] buttons = {
                (Button) rootView.findViewById(R.id.up_button),
                (Button) rootView.findViewById(R.id.left_button),
                (Button) rootView.findViewById(R.id.down_button),
                (Button) rootView.findViewById(R.id.right_button),
                (Button) rootView.findViewById(R.id.stop_button),
        };
        for(Button button : buttons){
            button.setOnClickListener(directionalistener);
        }

        rootView.findViewById(R.id.stop_button).setOnClickListener(stopListener);

        return rootView;
    }

    private boolean hasSphero(){
        if(mSphero != null){
            return true;
        }

        List<Sphero> robots = RobotProvider.getDefaultProvider().getRobots();
        Log.d("ManualDrive", "Got these robots: " + robots.toString());
        if(robots.size() > 0){
            mSphero = robots.get(0);
        } else {
            Log.d("ManualDrive", "No sphero connected");
            Toast.makeText(getActivity(), "No Sphero Connected", Toast.LENGTH_LONG);
        }

        if(mSphero != null){
            return true;
        }
        return false;
    }

    private class DirectionalClickListener implements View.OnClickListener{
        public void onClick(View v){
            if(!hasSphero()){ return; }

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
        }
    }

    private class StopClickListener implements View.OnClickListener{
        public void onClick(View v){
            if(!hasSphero()){ return; }
            mSphero.stop();
        }
    }
}
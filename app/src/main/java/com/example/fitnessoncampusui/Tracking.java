package com.example.fitnessoncampusui;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.support.annotation.TransitionRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tracking extends AppCompatActivity implements LocationListener, SensorEventListener {

    private static final String TAG = "Tracking";
    private static final String PROX_ALERT_INTENT = "com.example.fitnessoncampusui.PROXIMITY_ALERT";
    private static final String TRACK_FILENAME = "tracks.csv";
    private static final String TRAJECTORY_FILENAME = "trajectories.csv";

    private static final int PROXIMITY = 30;
    private static final int USER_ID = 14;

    // declare variables
    // represents the status of the app: 0 -- initial; 1 -- not tracking; 2 -- tracking
    private int appStatus = 0;
    int closeIndex = -1;
    private List<POI> POIs;
    private Map<String, Double> lastDistance;
    private List<Trajectory> Trajectories;
    private List<TrackRecord> TrackRecords;
    private POI TrackOrigin;
    private Location currentLocation;
    private int entering = -1;
    private int leaving = -1;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private BroadcastReceiver mBroadcastReceiver;

    float temperature;
    float angle;
    Date startTime;
    private float[] Gravity = new float[3];
    private float[] Rotation = new float[9];
    private float[] Inclination = new float[9];
    private float[] Magnetic = new float[3];
    private float[] Orientation = new float[3];
    private float north_azimuth = 0f;
    private float target_azimuth = 0f;
    private float currentAzimuth = 0f;
    private float currentTargetAzimuth = 0f;


    LinearLayout back;
    Dialog myDialog;
    Button trackBtn;
    ImageView compass, arrow;
    TextView tvName;
    TextView tvDistance;
    TextView tvDirection;
    TextView tvSpeed;
    TextView tvTemperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        Log.d(TAG, "onCreate: tracking");
        
        // initiailze variables
        back = (LinearLayout) findViewById(R.id.back);
        trackBtn = (Button) findViewById(R.id.btn_track);
        compass = (ImageView) findViewById(R.id.compass_bg2);
        arrow = (ImageView) findViewById(R.id.compass_arrow);
        tvName = (TextView) findViewById(R.id.poi_name);
        tvDistance = (TextView) findViewById(R.id.distance);
        tvDirection = (TextView) findViewById(R.id.direction);
        tvSpeed = (TextView) findViewById(R.id.speed);
        tvTemperature = (TextView) findViewById(R.id.temperature);

        POIs = new ArrayList<>();
        lastDistance = new HashMap<>();
        Trajectories = new ArrayList<>();
        TrackRecords = new ArrayList<>();
        TrackOrigin = new POI();
        myDialog = new Dialog(this);

        // click on back return to last activity
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Tracking.this, MainActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.slide_back_out, R.anim.slide_back_in);
            }
        });

        // Load POIs from raw
        loadPOI();
        Log.d(TAG, "onCreate: loaded "+POIs.size()+" number of POIs");

        // Check and make sure the location permission is granted
        checkLocationPermission();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String name;
                int tag;

                Bundle extras = intent.getExtras();

                if (extras != null) {
                    name = extras.getString("name");
                    tag = extras.getInt("alertTag");
                    showCardview(name, tag);
                }
            }
        };

        trackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: TRACKING");
                Log.d(TAG, "onClick: start tracking from " + POIs.get(entering));

                appStatus = 2;
                TrackOrigin = POIs.get(entering);

                TrackRecord start = new TrackRecord();
                start.setUser_id(USER_ID);
                start.setTrack_id(TrackRecords.size());
                start.setOrigin_name(TrackOrigin.getName());
                startTime = Calendar.getInstance().getTime();
                TrackRecords.add(start);

                // find the next closest poi and show info
                findClosestPOI(currentLocation);
                trackBtn.setEnabled(false);

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        addProximityAlert();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register broadcast receiver
        registerReceiver(mBroadcastReceiver, new IntentFilter(PROX_ALERT_INTENT));

        // check the avaiability of sensors and register for senser event listener
        Sensor gsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor msensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor tsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (gsensor != null) {
            mSensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_GAME);
        }

        if (msensor != null) {
            mSensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_GAME);
        }

        if (tsensor != null) {
            mSensorManager.registerListener(this, tsensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
        mSensorManager.unregisterListener(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationManager.removeUpdates(this);
        //writeToCSV();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        //final float alpha = 0.8f;

        final float alpha = 0.97f;

        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            // update temperature on the screen
            temperature=event.values[0];
            tvTemperature.setText(String.format("%.2f °C", temperature));

        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Gravity[0] = alpha*Gravity[0] + (1-alpha)*event.values[0];
            Gravity[1] = alpha*Gravity[1] + (1-alpha)*event.values[1];
            Gravity[2] = alpha*Gravity[2] + (1-alpha)*event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            Magnetic[0] = alpha*Magnetic[0] + (1-alpha)*event.values[0];
            Magnetic[1] = alpha*Magnetic[1] + (1-alpha)*event.values[1];
            Magnetic[2] = alpha*Magnetic[2] + (1-alpha)*event.values[2];
        }

        boolean success = SensorManager.getRotationMatrix(Rotation, Inclination, Gravity, Magnetic);
        if (success) {
            SensorManager.getOrientation(Rotation, Orientation);
            // get the azimuth relative to north
            north_azimuth =  (float) Math.toDegrees(Orientation[0]);
            // add the relative rotation from target POI to current location
            north_azimuth = (360+north_azimuth)%360;
            target_azimuth = north_azimuth - (360+angle)%360;

            // update direction on the screen
            tvDirection.setText(String.format("%.2f degree", angle));

            // TODO Rotation of the compass
            compassRotation( -currentAzimuth, -north_azimuth, compass);

            // TODO Rotation of the arrow pointing to target POI
            compassRotation(-currentTargetAzimuth, -target_azimuth, arrow);

            // set currentAzimuth to current angle
            currentAzimuth = north_azimuth;
            currentTargetAzimuth = target_azimuth;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        myDialog.dismiss();
        // for every points in the POI lists find the poi with the minimal distance to the location
        Log.d(TAG, "onLocationChanged: location updated!");

        // initialize variables
        double minDistance = Double.MAX_VALUE;
        currentLocation = location;
        entering = -1;
        leaving = -1;

        for (POI poi : POIs) {

            if (poi.getName() == TrackOrigin.getName() && appStatus == 2) continue;

            double distance = location.distanceTo(poi.getLocation());

            if (distance < minDistance) {
                minDistance = distance;
                closeIndex = POIs.indexOf(poi);
            }

            if (distance < PROXIMITY && lastDistance.get(poi.getName()) > PROXIMITY) {
                // entering
                entering = POIs.indexOf(poi);
                Log.d(TAG, "onLocationChanged: entering " + poi.getName() + ", distance: " + distance + ", last distance: " + lastDistance.get(poi.getName()));
            }

            if (distance > PROXIMITY && lastDistance.get(poi.getName()) < PROXIMITY) {
                // leaving
                leaving = POIs.indexOf(poi);
                Log.d(TAG, "onLocationChanged: leaving " + poi.getName() + ", distance: " + distance + ", last distance: " + lastDistance.get(poi.getName()));
            }

            lastDistance.put(poi.getName(), distance);

        }

        if (leaving != -1 && entering != -1) {
            Log.d(TAG, "onLocationChanged: Summary: entering " + POIs.get(entering).getName() + ", leaving " + POIs.get(leaving).getName());
        } else if (entering != -1) {
            Log.d(TAG, "onLocationChanged: Summary: entering "+ POIs.get(entering).getName());
        } else if (leaving != -1) {
            Log.d(TAG, "onLocationChanged: Summary: leaving " + POIs.get(leaving).getName());
        } else if (entering == -1 && leaving == -1) {
            Log.d(TAG, "onLocationChanged: Summary: no leaving or entering");
        }
        // update display information
        if (appStatus == 2) {
            Trajectories.add(new Trajectory(USER_ID, TrackRecords.size(), new Date(location.getTime()), location.getLongitude(), location.getLatitude(), location.getAltitude(), temperature));
            if (checkWritePermission()) {
                Trajectories.get(Trajectories.size() - 1).writeTrajectory(TRAJECTORY_FILENAME);
            }
        }

        angle = location.bearingTo(POIs.get(closeIndex).getLocation());
        updateInfo(closeIndex, minDistance, location.getSpeed());

        target_azimuth = north_azimuth - (360+angle)%360;

        compassRotation(-currentTargetAzimuth, -target_azimuth, arrow);

        currentTargetAzimuth = target_azimuth;

        Log.d(TAG, "onLocationChanged: closest POI: "+POIs.get(closeIndex).getName());
        // determine broadcast tag
        if (entering != -1) {
            // entering poi, whose index is entering
            if (appStatus == 2) {
                // end track
                Log.d(TAG, "onLocationChanged: END TRACK");
                TrackRecord currentTrack = TrackRecords.get(TrackRecords.size()-1);
                currentTrack.setDestination_name(POIs.get(entering).getName());
                currentTrack.setDuration(location.getTime()-startTime.getTime());
                if (checkWritePermission()) {
                    currentTrack.writeTrack(TRACK_FILENAME);
                }

                appStatus = 1;

                trackBtn.setEnabled(true);

                sendProximityIntent(currentTrack.getDestination_name(), 2);

            } else {
                // if is currently not tracking, alert ready
                Log.d(TAG, "onLocationChanged: READY TO GO");
                sendProximityIntent(POIs.get(entering).getName(), 1);
                trackBtn.setEnabled(true);
            }
        } else if (leaving != -1 && appStatus != 2) {
            // leaving POI and have not started tracking, alert leaving poi
            Log.d(TAG, "onLocationChanged: LEAVING");
            Log.d(TAG, "onLocationChanged: leaving " + POIs.get(leaving).getName());
            sendProximityIntent(POIs.get(leaving).getName(), 3);
            trackBtn.setEnabled(false);
        } else if (leaving == -1 && appStatus == 0) {
            // first launch the activity and not close to any pois, alert not found
            Log.d(TAG, "onLocationChanged: NOT FOUND");
            sendProximityIntent(POIs.get(closeIndex).getName(), 0);
            trackBtn.setEnabled(false);
            appStatus = 1;
        }


    }



    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    // HELPER FUNCTIONS

    /**
     * Check the permission for getting location. If permission is not granted, a pop up shows to ask for it
     *
     * @return True if permission granted
     *         False if no permissions granted
     */
    private boolean checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 2: {
                // if request is cancelled, the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // start the location update
                    // TODO switch between network and gps for location updates
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                }
            }
        }
    }

    /**
     * Load a list of POIs from raw and store in list POIs
     *
     * @return True if successfully load from file
     *         Error error message if encounter any error
     *         False if fail to load POIs
     */
    private boolean loadPOI() {
        try{
            String line = "";
            InputStream inputStream = getResources().openRawResource(R.raw.pois);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            int i = 0;
            while((line = reader.readLine()) != null) {
                if (i == 0) {
                    ++i;
                    continue;
                }
                String[] parse = line.split(";");

                Log.d(TAG, "loadPOI: parsed "+parse[0]+","+parse[1]+","+parse[2]+","+parse[3]);

                POI poi = new POI(parse[0], parse[1], Double.valueOf(parse[2]), Double.valueOf(parse[3]));
                Log.d(TAG, "loadPOI: finished");;
                POIs.add(poi);
                ++i;
            }
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Initialize proximity alert
     * for every poi, initialize lastDistance map with its name and distance (set to the max possible value)
     * register location manager for location updates with an interval of 1 sec or 5 m
     * throw any error encountered with the corresponding message
     */
    private void addProximityAlert() {
        try {

                for (POI poi : POIs){
                    lastDistance.put(poi.getName(), Double.MAX_VALUE);
                }
                // request for location updates
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5,this);

        } catch (SecurityException e) {
            Log.d("ERROR", e.getMessage());
        }
    }

    /**
     * Send corresponding intent with respect to different scenarios (i.e. alert "ready to go" when first entering a poi).
     *
     * @param name specify the name of poi entering/leaving;
     *             when neither entering or leaving, specify the name of the closest poi
     * @param alertTag specify four types of intent and the dialog to be shown
     *                 0 - initially launch the activity and not close to any poi
     *                 1 - ready to go, previously not in tracking and enter one of the poi
     *                 2 - finish tracking, previously in tracking and enter one of the poi to stop tracking
     *                 3 - leaving the poi, which previously within the proximity and have not yet started tracking,
     */
    private void sendProximityIntent(String name, int alertTag) {
        Intent i = new Intent(PROX_ALERT_INTENT);
        i.putExtra("name", name);
        i.putExtra("alertTag", alertTag);

        sendBroadcast(i);
    }

    /**
     * Update the information on the screen
     * Note: the updating interval for these information is the same as location updates,
     * while the temperature will be updated whenever the temperature sensor received a change
     *
     * @param index denotes the index of the closest poi, use this parameter to find the name of the poi to be shown
     * @param minDistance denotes the distance to the closet poi
     * @param speed denotes the current speed
     */
    private void updateInfo(int index, double minDistance, double speed) {

        tvName.setText(POIs.get(index).getName());
        tvDistance.setText(String.format("%.2f m", minDistance));
        tvDirection.setText(String.format("%.2f degree", (angle+360)%360));
        tvSpeed.setText(String.format("%.2f m/s", speed));
    }

    /**
     * Rotate the image from current angle to a specified angle
     *
     * @param from denotes the previous angle of the image
     * @param to denotes the ending angle of this rotation
     * @param img denotes the image to be rotated
     */
    private void compassRotation(float from, float to, ImageView img) {
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(from, to, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        // how long the animation will take place
        ra.setDuration(500);
        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        ra.setRepeatCount(0);
        // Start the animation
        img.startAnimation(ra);
    }

    /**
     * Show correpsonding card as a dialog given the tag of the alert
     *
     * @param name specifies the name of poi to be shown on the card
     * @param tag specifies the type of card to be shown
     */
    public void showCardview(String name, int tag) {

        TextView poiName;

        if (tag == 0) {
            // close POI not found
            myDialog.setContentView(R.layout.popup_notfound);
            poiName = (TextView) myDialog.findViewById(R.id.poi_name_nf);
            poiName.setText(name);

        } else if (tag == 1) {
            // within POI, ready to go
            myDialog.setContentView(R.layout.pop_readytogo);
            poiName = (TextView) myDialog.findViewById(R.id.poi_name_rd);
            poiName.setText(name);

        } else if (tag == 2) {
            // end journey, show summary
            myDialog.setContentView(R.layout.popup_finish);

            TextView origin = (TextView) myDialog.findViewById(R.id.poi_name_from);
            final TextView destination = (TextView) myDialog.findViewById(R.id.poi_name_to);
            TextView duration = (TextView) myDialog.findViewById(R.id.popup_duration);
            Button restart = (Button) myDialog.findViewById(R.id.btn_restart);

            final TrackRecord lastTrack = TrackRecords.get(TrackRecords.size()-1);
            origin.setText(lastTrack.getOrigin_name());
            destination.setText(lastTrack.getDestination_name());

            double minute = lastTrack.getDuration()/6000;
            duration.setText(String.format("%.1f min", minute));
            appStatus = 1;
            TrackOrigin = new POI();

            restart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: TRACKING");
                    appStatus = 2;
                    TrackOrigin = POIs.get(entering);
                    TrackRecord start = new TrackRecord();
                    start.setUser_id(USER_ID);
                    start.setTrack_id(TrackRecords.size());
                    start.setOrigin_name(TrackOrigin.getName());
                    startTime = Calendar.getInstance().getTime();
                    TrackRecords.add(start);

                    // update information to the next cloest poi
                    findClosestPOI(currentLocation);

                    trackBtn.setEnabled(false);

                    myDialog.dismiss();
                }
            });

        } else if (tag == 3) {
            // leaving POI, show leaving notification
            myDialog.setContentView(R.layout.popup_leaving);
            poiName = (TextView) myDialog.findViewById(R.id.poi_name_lv);
            poiName.setText(name);
        }

        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        myDialog.show();

    }

    private void writeToCSV() {

        // Saving users input to a CSV file
        if(checkWritePermission()&&Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

            File directory = Environment.getExternalStorageDirectory();

            try{
                // write trajectoy files
                File file = new File (directory, "trajectories.csv");
                Log.d(TAG, "writeToCSV: start writing trajectories");

                FileOutputStream outputStream = new FileOutputStream(file, true);
                PrintWriter writer = new PrintWriter(outputStream);

                for (Trajectory t : Trajectories) {
                    Log.d(TAG, "writeToCSV: writing trajectory "+ Trajectories.indexOf(t));

                    writer.print(t.getUser_id()+ ",");
                    writer.print(t.getTrack_id() + ",");
                    writer.print(t.getTime() + ",");
                    writer.print(t.getLongitude() + ",");
                    writer.print(t.getLatitude() + ",");
                    writer.print(t.getAltitude() + ",");
                    writer.println(t.getTemperature());
                }

                writer.close();
                outputStream.close();
                Log.d(TAG, "writeToCSV: successfully writed "+Trajectories.size()+" trajectory points");

            }catch(IOException e){
                Log.e(TAG, "writeToCSV: failed write to trajectories.csv");
            }

            try{
                File file = new File (directory, "tracks.csv");

                Log.d(TAG, "writeToCSV: start writing tracks");

                FileOutputStream outputStream = new FileOutputStream(file, true);
                PrintWriter writer = new PrintWriter(outputStream);

                for (TrackRecord tk : TrackRecords) {
                    Log.d(TAG, "writeToCSV: writing track "+TrackRecords.indexOf(tk));
                    writer.print(tk.getUser_id() + ",");
                    writer.print(tk.getTrack_id() + ",");
                    writer.print(tk.getOrigin_name() + ",");
                    writer.print(tk.getDestination_name() + ",");
                    writer.println(tk.getDuration());
                }


                writer.close();
                outputStream.close();

                Log.d(TAG, "writeToCSV: successully write "+TrackRecords.size()+" tracks to " + file.getPath());

            }catch(IOException e){
                Log.e(TAG, "writeToCSV: failed to write tracks");

            }

        }else{
            Log.e(TAG, "writeToCSV: SD card not mounted");

        }

    }

    /**
     * Check the permission for writing trajectories and tracks to external storage.
     * If the permission is not yet granted, request for it in a popup
     *
     * @return true if granted
     *         false if not granted
     */
    public boolean checkWritePermission() {
        Log.d(TAG, "checkWritePermission: starts");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},3);
            return false;
        } else {
            Log.d(TAG, "checkWritePermission: permission granted");
            return true;
        }
    }

    /**
     * find the next closest poi to the current location and update the corresponding information
     *
     * @param location the current location, the closest poi will be calculated based on this location
     */
    private void findClosestPOI(Location location) {

        double minDistance = Double.MAX_VALUE;

        for (POI poi : POIs) {

            double distance = location.distanceTo(poi.getLocation());

            if (poi.getName() == TrackOrigin.getName()) {
                Log.d(TAG, "onLocationChanged: TrackOrigin " + TrackOrigin.getName() + ", skip " + poi.getName());
                continue;
            }

            if (distance < minDistance) {
                minDistance = distance;
                closeIndex = POIs.indexOf(poi);
            }

            Log.d(TAG, "onLocationChanged: "+poi.getName()+": "+distance);
        }

        angle = location.bearingTo(POIs.get(closeIndex).getLocation());
        updateInfo(closeIndex, minDistance, location.getSpeed());

        target_azimuth = north_azimuth - (360+angle)%360;

        compassRotation(-currentTargetAzimuth, -target_azimuth, arrow);

        currentTargetAzimuth = target_azimuth;
    }

}

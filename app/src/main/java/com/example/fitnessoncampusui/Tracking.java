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

    private static final int PROXIMITY = 50;
    private static final int USER_ID = 14;

    // declare variables
    // represents the status of the app: 0 -- initial; 1 -- not tracking; 2 -- tracking
    private int appStatus = 0;
    int closeIndex = -1;
    private List<POI> POIs;
    //private List<Geofence> geofences;
    private Map<String, Double> lastDistance;
    private List<Trajectory> Trajectories;
    private List<Track> Tracks;
    private List<TrackRecord> TrackRecords;
    private POI TrackOrigin;
    private Location currentLocation;
    private double minDistantlast;

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
        minDistantlast = Double.MAX_VALUE;
        //geofences = new ArrayList<>();
        Trajectories = new ArrayList<>();
        Tracks = new ArrayList<>();
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
                Log.d(TAG, "onClick: start tracking from " + POIs.get(closeIndex));

                appStatus = 2;
                TrackOrigin = POIs.get(closeIndex);
                //minDistantlast = Double.MAX_VALUE;

                TrackRecord start = new TrackRecord();
                start.setUser_id(USER_ID);
                start.setTrack_id(TrackRecords.size());
                start.setOrigin_name(TrackOrigin.getName());
                startTime = Calendar.getInstance().getTime();
                TrackRecords.add(start);

                //lastDistance.put(POIs.get(closeIndex).getName(), distance);
                // find the next closest poi and show info
                findClosestPOI(currentLocation);
                /*
                double minDistance = Double.MAX_VALUE;
                double distance = 0;

                for (POI poi : POIs) {
                    if (poi.getName() == TrackOrigin.getName()) {
                        continue;
                    }
                    distance = currentLocation.distanceTo(poi.getLocation());
                    if (distance  < minDistance) {
                        minDistance = distance;
                        closeIndex = POIs.indexOf(poi);
                    }
                }

                angle = currentLocation.bearingTo(POIs.get(closeIndex).getLocation());
                updateInfo(closeIndex, minDistance, currentLocation.getSpeed());
*/
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
        writeToCSV();

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
            //if (north_azimuth * angle < 0) target_azimuth = north_azimuth - (float) angle;
            //else target_azimuth = (float) angle - north_azimuth;

            north_azimuth = (360+north_azimuth)%360;
            target_azimuth = north_azimuth - (360+angle)%360;

            // update direction on the screen
            //tvDirection.setText(String.format("%.2f degree", angle));

            //showRotation.setText("Heading: " + Double.toString(north_azimuth) + " degrees ; " + Double.toString(target_azimuth) + " degrees to " + POIs.get(index).getName());

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

        double minDistance = findClosestPOI(location);
        //double minDistance = Double.MAX_VALUE;
/*
        for (POI poi : POIs) {

            double distance = location.distanceTo(poi.getLocation());

            if (poi.getName() == TrackOrigin.getName() && appStatus == 2) continue;

            if (distance < minDistance) {
                minDistance = distance;
                closeIndex = POIs.indexOf(poi);
            }

        }
*/
        Log.d(TAG, "onLocationChanged: closest poi "+ POIs.get(closeIndex).getName()+", distance: "+minDistance+", lastDistance: "+minDistantlast);

        if (minDistance < PROXIMITY && minDistantlast > PROXIMITY) {
            // entering poi
            if (appStatus == 2) {
                // end track
                TrackRecord currentTrack = TrackRecords.get(TrackRecords.size()-1);
                currentTrack.setDestination_name(POIs.get(closeIndex).getName());
                Log.d(TAG, "onLocationChanged: set duration: "+(location.getTime()-startTime.getTime()));
                currentTrack.setDuration(location.getTime()-startTime.getTime());
                Log.d(TAG, "onLocationChanged: duration: "+currentTrack.getDuration());

                appStatus = 1;
                //minDistantlast = Double.MAX_VALUE;
                trackBtn.setEnabled(true);

                Log.d(TAG, "onLocationChanged: startime: "+startTime.getTime()+"; endtime: "+location.getTime());
                Log.d(TAG, "onLocationChanged: currenttime"+Calendar.getInstance().getTime());


                sendProximityIntent(currentTrack.getDestination_name(), 2, location);
            } else {
                // if is currently not tracking, alert ready
                sendProximityIntent(POIs.get(closeIndex).getName(), 1, location);
                trackBtn.setEnabled(true);
            }
        } else if (minDistance > PROXIMITY && minDistantlast < PROXIMITY && appStatus != 2) {
            // leaving POI and have not started tracking, alert not found
            Log.d(TAG, "onLocationChanged: leaving " + POIs.get(closeIndex));
            sendProximityIntent(POIs.get(closeIndex).getName(), 3, location);
            trackBtn.setEnabled(false);
        } else if (minDistance > PROXIMITY && minDistantlast > PROXIMITY && appStatus == 0) {
            // first launch the activity and not close to any pois, alert not found
            Log.d(TAG, "onLocationChanged: not found");
            sendProximityIntent(POIs.get(closeIndex).getName(), 0, location);
            trackBtn.setEnabled(false);
            appStatus = 1;
            minDistantlast = Double.MAX_VALUE;
        }

        //minDistantlast = minDistance;


//        Log.d(TAG, "onLocationChanged: close POI: "+POIs.get(closeIndex).getName()+", minDistance = "+minDistance+", lastDistance"+lastDistance.get(POIs.get(closeIndex).getName()));
        currentLocation = location;
/*
        for (Geofence g : geofences) {

            double distance = 0.0;

            distance = g.getLocation().distanceTo(location);

            if (appStatus == 2 && g.getName() == TrackOrigin.getName()) {
                Log.d(TAG, "onLocationChanged: TrackOrigin "+TrackOrigin.getName()+", skip "+g.getName());
                continue;
            }

            if (distance < minDistance) {
                minDistance = distance;
                closeIndex = g.getIndex();
            }

            Log.d(TAG, "onLocationChanged: "+g.getName()+": "+distance);
        }
*/
/*
        if (minDistance < PROXIMITY && lastDistance.get(POIs.get(closeIndex).getName()) > PROXIMITY) {
            // entering geofence
            Log.d(TAG, "onLocationChanged: entering " + POIs.get(closeIndex).getName());
            if (appStatus == 2)  {
                // currently in tracking mode, alert end journey
                // populate track data

                TrackRecord currentTrack = TrackRecords.get(TrackRecords.size()-1);
                currentTrack.setDestination_name(POIs.get(closeIndex).getName());
                Log.d(TAG, "onLocationChanged: set duration: "+(location.getTime()-startTime.getTime()));
                currentTrack.setDuration(location.getTime()-startTime.getTime());
                Log.d(TAG, "onLocationChanged: duration: "+currentTrack.getDuration());

                appStatus = 1;
                trackBtn.setEnabled(true);

                Log.d(TAG, "onLocationChanged: startime: "+startTime.getTime()+"; endtime: "+location.getTime());
                Log.d(TAG, "onLocationChanged: currenttime"+Calendar.getInstance().getTime());


                sendProximityIntent(currentTrack.getDestination_name(), 2, location);

            }
            else {
                // if is currently not tracking, alert ready
                sendProximityIntent(POIs.get(closeIndex).getName(), 1, location);
                trackBtn.setEnabled(true);
            }
        } else if (minDistance > PROXIMITY && lastDistance.get(POIs.get(closeIndex).getName()) < PROXIMITY && appStatus != 2) {
            // leaving POI and have not started tracking, alert not found
            Log.d(TAG, "onLocationChanged: leaving " + POIs.get(closeIndex));
            sendProximityIntent(POIs.get(closeIndex).getName(), 3, location);
            trackBtn.setEnabled(false);
        } else if (minDistance > PROXIMITY && lastDistance.get(POIs.get(closeIndex).getName()) > PROXIMITY && appStatus == 0 ) {
            // first launch the activity and not close to any pois, alert not found
            Log.d(TAG, "onLocationChanged: not found");
            sendProximityIntent(POIs.get(closeIndex).getName(), 0, location);
            trackBtn.setEnabled(false);
            appStatus = 1;
        }

        lastDistance.put(POIs.get(closeIndex).getName(), minDistance);
*/

        /*

        if (appStatus == 2) {
            Trajectories.add(new Trajectory(USER_ID, Tracks.size(), new Date(location.getTime()), location.getLongitude(), location.getLatitude(), location.getAltitude(), temperature));
        }

        angle = location.bearingTo(POIs.get(closeIndex).getLocation());
        lastDistance.put(POIs.get(closeIndex).getName(), minDistance);

        updateInfo(closeIndex, minDistance, location.getSpeed());
        */

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
     * Check for permission. If permission is not granted, a pop up shows to ask for it
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

    private void addProximityAlert() {
        try {

                for (POI poi : POIs){
                    /*
                    Geofence g = new Geofence(i, poi.getLatitude(), poi.getLongitude(), poi.getName());
                    geofences.add(g);
                    Log.d(TAG, "addProximityAlert: added poi "+poi.getName() + "to Geofences");
                    */
                    lastDistance.put(poi.getName(), Double.MAX_VALUE);
                }

                // request for location updates
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5,this);

        } catch (SecurityException e) {
            Log.d("ERROR", e.getMessage());
        }
    }

    private void sendProximityIntent(String name, int alertTag, Location location) {
        Intent i = new Intent(PROX_ALERT_INTENT);
        i.putExtra("name", name);
        i.putExtra("alertTag", alertTag);
        i.putExtra("lat", location.getLatitude());
        i.putExtra("lng", location.getLongitude());

        sendBroadcast(i);
    }

    private void updateInfo(int index, double minDistance, double speed) {

        tvName.setText(POIs.get(index).getName());
        tvDistance.setText(String.format("%.2f m", minDistance));
        tvDirection.setText(String.format("%.2f degree", (angle+360)%360));
        tvSpeed.setText(String.format("%.2f m/s", speed));
    }

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
            Log.d(TAG, "showCardview: duration: "+minute);
            duration.setText(String.format("%.1f min", minute));
            appStatus = 1;
            //minDistantlast = Double.MAX_VALUE;

            restart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appStatus = 2;
                    TrackOrigin = POIs.get(closeIndex);
                    TrackRecord start = new TrackRecord();
                    start.setUser_id(USER_ID);
                    start.setTrack_id(TrackRecords.size());
                    start.setOrigin_name(TrackOrigin.getName());
                    startTime = Calendar.getInstance().getTime();
                    TrackRecords.add(start);

                    findClosestPOI(currentLocation);

                    //minDistantlast = Double.MAX_VALUE;

                    // find the next closest poi and show info
                    /*
                    double minDistance = Double.MAX_VALUE;
                    double distance = 0;

                    for (POI poi : POIs) {
                        if (poi.getName() == TrackOrigin.getName()) {
                            continue;
                        }
                        distance = currentLocation.distanceTo(poi.getLocation());
                        if (distance  < minDistance) {
                            minDistance = distance;
                            closeIndex = POIs.indexOf(poi);
                        }
                    }

                    angle = currentLocation.bearingTo(POIs.get(closeIndex).getLocation());
                    updateInfo(closeIndex, minDistance, currentLocation.getSpeed());
*/
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
        if(checckWritePermission()&&Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

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

                /*
                for (Track tk : Tracks) {
                    Log.d(TAG, "writeToCSV: writing track "+Tracks.indexOf(tk));
                    writer.print(tk.getUser_id() + ",");
                    writer.print(tk.getTrack_id() + ",");
                    writer.print(tk.getStartPOI().getName() + ",");
                    writer.print(tk.getEndPOI().getName() + ",");
                    writer.println(tk.duration());
                }
                */
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

                Log.d(TAG, "writeToCSV: successully write "+Tracks.size()+" tracks to " + file.getPath());

            }catch(IOException e){
                Log.e(TAG, "writeToCSV: failed to write tracks");

            }


        }else{
            Log.e(TAG, "writeToCSV: SD card not mounted");

        }

    }

    private boolean checckWritePermission() {
        Log.d(TAG, "checckWritePermission: starts");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},3);
            return false;
        } else {
            Log.d(TAG, "checckWritePermission: permission granted");
            return true;
        }
    }

    private double findClosestPOI(Location location) {

        double minDistance = Double.MAX_VALUE;

        double distance = 0.0;

        for (POI poi : POIs) {

            distance = location.distanceTo(poi.getLocation());

            if (appStatus == 2 && poi.getName() == TrackOrigin.getName()) {
                Log.d(TAG, "onLocationChanged: TrackOrigin " + TrackOrigin.getName() + ", skip " + poi.getName());
                lastDistance.put(poi.getName(),distance);
                continue;
            }

            if (distance < minDistance) {
                minDistance = distance;
                closeIndex = POIs.indexOf(poi);
                minDistantlast = lastDistance.get(poi.getName());
            }

            lastDistance.put(poi.getName(),distance);

            Log.d(TAG, "onLocationChanged: "+poi.getName()+": "+distance);
        }

        if (appStatus == 2) {
            Trajectories.add(new Trajectory(USER_ID, Tracks.size(), new Date(location.getTime()), location.getLongitude(), location.getLatitude(), location.getAltitude(), temperature));
        }

        angle = location.bearingTo(POIs.get(closeIndex).getLocation());
        updateInfo(closeIndex, minDistance, location.getSpeed());

        target_azimuth = north_azimuth - (360+angle)%360;

        compassRotation(-currentTargetAzimuth, -target_azimuth, arrow);

        currentTargetAzimuth = target_azimuth;


        return minDistance;

    }

}

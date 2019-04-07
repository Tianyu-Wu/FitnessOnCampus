package com.example.fitnessoncampusui;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class Trajectory {
    private static final String TAG = "Trajectory";

    private int user_id;
    private int track_id;
    private Date time;
    private double longitude;
    private double latitude;
    private double altitude;
    private float temperature;

    public Trajectory(int user_id, int track_id, Date time, double longitude, double latitude, double altitude, float temperature) {
        this.user_id = user_id;
        this.track_id = track_id;
        this.time = time;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.temperature = temperature;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getTrack_id() {
        return track_id;
    }

    public void setTrack_id(int track_id) {
        this.track_id = track_id;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void writeTrajectory(String filename) {
        // Saving users input to a CSV file
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

            File directory = Environment.getExternalStorageDirectory();

            try{
                // write trajectoy files
                File file = new File (directory, filename);
                Log.d(TAG, "writeToCSV: start writing trajectories");

                FileOutputStream outputStream = new FileOutputStream(file, true);
                PrintWriter writer = new PrintWriter(outputStream);

                writer.print(user_id+ ",");
                writer.print(track_id + ",");
                writer.print(time + ",");
                writer.print(longitude + ",");
                writer.print(latitude + ",");
                writer.print(altitude + ",");
                writer.println(temperature);

                writer.close();
                outputStream.close();
                Log.d(TAG, "writeToCSV: successfully wrote trajectory point to "+file.getPath());

            }catch(IOException e){
                Log.e(TAG, "writeToCSV: failed write to trajectories.csv");
            }

        }else{
            Log.e(TAG, "writeToCSV: SD card not mounted");

        }
    }
}

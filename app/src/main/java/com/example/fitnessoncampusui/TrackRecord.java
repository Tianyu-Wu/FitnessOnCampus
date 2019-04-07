package com.example.fitnessoncampusui;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;


public class TrackRecord {

    private static final String TAG = "TrackRecord";

    private int user_id;
    private int track_id;
    private String origin_name;
    private String destination_name;
    private Long duration;

    public TrackRecord() {
    }

    public TrackRecord(int user_id, int track_id, String origin_name, String destination_name, Long duration) {
        this.user_id = user_id;
        this.track_id = track_id;
        this.origin_name = origin_name;
        this.destination_name = destination_name;
        this.duration = duration;
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

    public String getOrigin_name() {
        return origin_name;
    }

    public void setOrigin_name(String origin_name) {
        this.origin_name = origin_name;
    }

    public String getDestination_name() {
        return destination_name;
    }

    public void setDestination_name(String destination_name) {
        this.destination_name = destination_name;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public void writeTrack(String filename) {
        // Saving users input to a CSV file
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

            File directory = Environment.getExternalStorageDirectory();

            try{
                File file = new File (directory, filename);

                Log.d(TAG, "writeToCSV: start writing tracks");

                FileOutputStream outputStream = new FileOutputStream(file, true);
                PrintWriter writer = new PrintWriter(outputStream);

                writer.print(user_id + ",");
                writer.print(track_id + ",");
                writer.print(origin_name + ",");
                writer.print(destination_name + ",");
                writer.println(duration);

                writer.close();
                outputStream.close();

                Log.d(TAG, "writeToCSV: successully write tracks to " + file.getPath());

            }catch(IOException e){
                Log.e(TAG, "writeToCSV: failed to write tracks");

            }

        }else{
            Log.e(TAG, "writeToCSV: SD card not mounted");

        }

    }
}

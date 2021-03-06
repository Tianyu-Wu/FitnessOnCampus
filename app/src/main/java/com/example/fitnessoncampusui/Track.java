package com.example.fitnessoncampusui;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class Track {

    private int user_id;
    private int track_id;
    private POI startPOI;
    private POI endPOI;
    private Date startTime;
    private Date endTime;

    public Track(int user_id, int track_id, POI startPOI, POI endPOI, Date startTime, Date endTime) {
        this.user_id = user_id;
        this.track_id = track_id;
        this.startPOI = startPOI;
        this.endPOI = endPOI;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Track() {

    }

    public int getUser_id() {
        return user_id;
    }

    public int getTrack_id() {
        return track_id;
    }

    public POI getStartPOI() {
        return startPOI;
    }

    public POI getEndPOI() {
        return endPOI;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public void setTrack_id(int track_id) {
        this.track_id = track_id;
    }

    public void setStartPOI(POI startPOI) {
        this.startPOI = startPOI;
    }

    public void setEndPOI(POI endPOI) {
        this.endPOI = endPOI;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public double duration() {
        return endTime.getTime() - startTime.getTime();
    }

    public void outputFiles() {
        // Saving users input to a CSV file
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            try{
                Log.d("FileLog", "start writing tracks ");

                File directory = Environment.getExternalStorageDirectory();

                File file = new File (directory, "tracks.csv");

                FileOutputStream outputStream = new FileOutputStream(file, true);
                PrintWriter writer = new PrintWriter(outputStream);

                Log.d("FileLog", "start writing trajectory");
                writer.print(user_id + ",");
                writer.print(track_id + ",");
                writer.print(startPOI.getName() + ",");
                writer.print(endPOI.getName() + ",");
                writer.println(duration());

                writer.close();
                outputStream.close();
                Log.e("FileLog", "tracks.csv Saved :  " + file.getPath());

            }catch(IOException e){
                Log.e("FileLog", "File to write tracks");
            }
        }else{
            Log.e("FileLog", "SD card not mounted");
        }
    }

}

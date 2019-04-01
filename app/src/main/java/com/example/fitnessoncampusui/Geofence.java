package com.example.fitnessoncampusui;

import android.location.Location;

public class Geofence {
    private int index;
    private double latitude;
    private double longitude;
    private String name;

    public Geofence(int index, double latitude, double longitude, String name) {
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public Location getLocation() {
        Location target = new Location("");
        target.setLatitude(latitude);
        target.setLongitude(longitude);
        return target;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

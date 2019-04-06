package com.example.fitnessoncampusui;

public class TrackRecord {

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
}

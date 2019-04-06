package com.example.fitnessoncampusui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.ViewHolder> {

    private static final String TAG = "RecycleViewAdapter";

    private List<TrackRecord> tracks = new ArrayList<>();
    private Context mContext;

    public RecycleViewAdapter(List<TrackRecord> tracks, Context mContext) {
        this.tracks = tracks;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.track_row, viewGroup, false);
        ViewHolder holder = new ViewHolder(view);

        return holder;

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Log.d(TAG, "onBindViewHolder: called");

        int index = tracks.size()-i-1;

        viewHolder.origin.setText(tracks.get(index).getOrigin_name());
        viewHolder.destination.setText(tracks.get(index).getDestination_name());
        viewHolder.trackID.setText(Integer.toString(tracks.get(index).getTrack_id()));
        viewHolder.userID.setText(Integer.toString(tracks.get(index).getUser_id()));
        double minute = tracks.get(index).getDuration()/6000;
        viewHolder.duration.setText(String.format("%.1f min", minute));

    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView origin;
        TextView destination;
        TextView trackID;
        TextView userID;
        TextView duration;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            origin = itemView.findViewById(R.id.origin);
            destination = itemView.findViewById(R.id.destination);
            trackID = itemView.findViewById(R.id.trackID);
            userID = itemView.findViewById(R.id.userID);
            duration = itemView.findViewById(R.id.duration);

        }
    }


}

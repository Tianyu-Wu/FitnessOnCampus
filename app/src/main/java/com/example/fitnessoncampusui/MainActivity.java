package com.example.fitnessoncampusui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Show previous tracks in recycler view with cardview layout
 * the tracks are shown in a reverse sequence (i.e. the most recent one shows up at the top)
 */
public class MainActivity extends AppCompatActivity {

    TextView title;
    Animation topDown, bottomUp;
    Button startBtn;
    RelativeLayout emptyLayout;


    List<TrackRecord> trackRecords;
    final static String TRACK_FILENAME = "tracks.csv";

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        title = (TextView) findViewById(R.id.title);
        startBtn = (Button) findViewById(R.id.btn_start);

        // load two animations
        bottomUp = AnimationUtils.loadAnimation(this, R.anim.bottomup);
        topDown = AnimationUtils.loadAnimation(this, R.anim.topdown);
        trackRecords = new ArrayList<>();


        Log.d(TAG, "onCreate: started");


        // click on the button, launch next activity for tracking
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainActivity.this, Tracking.class);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart: started");

        // Load and show previous tracks
        showTrack();

    }

    /**
     * Read previous tracks from tracks.csv in external storage directory
     */
    private void loadTracks() {

        if (checkWritePermission() && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                String line = "";
                File directory = Environment.getExternalStorageDirectory();
                Log.d(TAG, "loadTracks: filepath = "+directory.getPath());
                File file = new File(directory, TRACK_FILENAME);

                Log.d(TAG, "loadTracks: track.csv exist - "+file.getPath());
                //boolean deleted = file.delete();
                //Log.d(TAG, "loadTracks: track.csv deleted "+deleted);

                if (file.exists()) {
                    Log.d(TAG, "loadTracks: start reading");
                    FileInputStream inputStream = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    int i = 0;
                    while ((line = reader.readLine()) != null) {

                        Log.d(TAG, "loadTracks: line: "+line);
                        String[] parse = line.split(",");

                        TrackRecord track = new TrackRecord(Integer.valueOf(parse[0]), Integer.valueOf(parse[1]), parse[2], parse[3], Long.valueOf(parse[4]));
                        trackRecords.add(track);

                        ++i;

                        Log.d(TAG, "loadTracks: loaded "+i+" tracks");
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * since when write permission is granted, the read permission is automatically granted and the write permission will be used
     * in the next activity, here we check the write permission directly
     * @return
     */
    public boolean checkWritePermission() {
        Log.d(TAG, "checkReadPermission: starts");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            return false;
        } else {
            Log.d(TAG, "checkReadPermission: permission granted");
            return true;
        }
    }

    private void showTrack() {
        Log.d(TAG, "showTrack: verify");

        // Load previous track records
        loadTracks();

        if (trackRecords.size() > 0) {
            // Show previous tracks with recyclerview and cardview
            initRecyclerView();
        } else {
            // if there is no records in tracks.csv or if the file does not exists, show the image denoting no records
            emptyLayout = (RelativeLayout) findViewById(R.id.empty_layout);
            if (emptyLayout.getVisibility() == View.INVISIBLE) {

                topDown.setStartOffset(400);
                emptyLayout.startAnimation(topDown);
                emptyLayout.setVisibility(View.VISIBLE);
            }
        }

    }


    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: init");
        RecyclerView recyclerView = findViewById(R.id.mRecyclerView);
        RecycleViewAdapter adapter = new RecycleViewAdapter(trackRecords, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }
}

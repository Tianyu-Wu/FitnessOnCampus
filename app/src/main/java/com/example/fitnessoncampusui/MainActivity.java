package com.example.fitnessoncampusui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
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

public class MainActivity extends AppCompatActivity {

    ImageView background;
    LinearLayout welcome_contents;
    TextView title;
    Animation bottomUp, topDown;
    Button startBtn;
    RelativeLayout emptyLayout;


    List<TrackRecord> trackRecords;
    final static String TRACK_FILENAME = "tracks.csv";

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "onCreate: started");

        background = (ImageView) findViewById(R.id.background);
        welcome_contents = (LinearLayout) findViewById(R.id.welcome_contents);
        title = (TextView) findViewById(R.id.title);
        startBtn = (Button) findViewById(R.id.btn_start);
        bottomUp = AnimationUtils.loadAnimation(this, R.anim.bottomup);
        topDown = AnimationUtils.loadAnimation(this, R.anim.topdown);
        trackRecords = new ArrayList<>();

        // Welcome screen animation
        background.animate().translationY(-2020).setDuration(800).setStartDelay(400);
        welcome_contents.animate().translationY(140).alpha(0).setDuration(800).setStartDelay(500);
        title.startAnimation(bottomUp);
        startBtn.startAnimation(bottomUp);
        //title.animate().translationY(40).alpha(1).setDuration(800).setStartDelay(2000);

        // Load and show previous tracks
        showTrack();

        // TODO Button action
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, Tracking.class);
                startActivity(i);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart: started");


    }

    private void loadTracks() {

        if (checkWritePermission() && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                String line = "";
                File directory = Environment.getExternalStorageDirectory();
                File file = new File(directory, TRACK_FILENAME);

                if (file.exists()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    int i = 0;
                    while ((line = reader.readLine()) != null) {
                        if (i == 0) {
                            ++i;
                            continue;
                        }
                        String[] parse = line.split(",");

                        TrackRecord track = new TrackRecord(Integer.valueOf(parse[0]), Integer.valueOf(parse[1]), parse[2], parse[3], Double.valueOf(parse[4])/60000);
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

    private boolean checkWritePermission() {
        Log.d(TAG, "checkWritePermission: starts");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
            return false;
        } else {
            Log.d(TAG, "checkWritePermission: permission granted");
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
            emptyLayout = (RelativeLayout) findViewById(R.id.empty_layout);
            if (emptyLayout.getVisibility() == View.INVISIBLE) {

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

package com.example.fitnessoncampusui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class Tracking extends AppCompatActivity {

    private static final String TAG = "Tracking";

    // declare variables
    private List<POI> POIs;


    LinearLayout back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        Log.d(TAG, "onCreate: tracking");
        
        back = (LinearLayout) findViewById(R.id.back);
        
        // click on back return to last activity
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Tracking.this, MainActivity.class);
                startActivity(i);
            }
        });

        
    }


    private boolean loadPOI(List<POI> pois) {
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
                Log.d("MainActivity", parse[0]);
                Log.d("MainActivity", parse[1]);
                Log.d("MainActivity", parse[2]);
                Log.d("MainActivity", parse[3]);

                POI poi = new POI(parse[0], parse[1], Double.valueOf(parse[3]), Double.valueOf(parse[2]));
                Log.d(TAG, "loadPOI: finished");;
                pois.add(poi);
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
}

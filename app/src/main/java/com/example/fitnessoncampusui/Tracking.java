package com.example.fitnessoncampusui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class Tracking extends AppCompatActivity {

    private static final String TAG = "Tracking";
    
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
}

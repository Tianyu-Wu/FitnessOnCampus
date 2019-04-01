package com.example.fitnessoncampusui;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashScreen extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 1000;
    private static final String TAG = "SplashScreen";

    ImageView background;
    LinearLayout welcome_contents;
    Animation topDown;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        Log.d(TAG, "onCreate: splash screen launched");
        background = (ImageView) findViewById(R.id.background);
        welcome_contents = (LinearLayout) findViewById(R.id.welcome_contents);

        background.animate().translationY(-2020).setDuration(SPLASH_DISPLAY_LENGTH).setStartDelay(400);
        welcome_contents.animate().translationY(140).alpha(0).setDuration(SPLASH_DISPLAY_LENGTH).setStartDelay(500);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                //finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }
}

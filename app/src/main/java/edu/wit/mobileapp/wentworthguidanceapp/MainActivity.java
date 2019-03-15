package edu.wit.mobileapp.wentworthguidanceapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class MainActivity extends AppCompatActivity {
    private final String dummyLog = "PlaceHolderLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Button GP2Button = (Button) findViewById(R.id.activity3Button);

    }

    public void switchToActivity(View view)
    {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
    }

    public void nfcConnect(View view)
    {
        Log.v(dummyLog,"The NFC connection will take place here!");
        switchToActivity(view);
    }
}

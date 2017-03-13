package com.xa.mapdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by XA on 3/11/2017.
 *
 */

public class BestRoadActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_best);
        Intent intent = getIntent();
        boolean if_use_location = intent.getBooleanExtra("if_use_location",true);
        Log.i("BestRoadActivity", if_use_location + "");

    }
}

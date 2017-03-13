package com.xa.mapdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private CheckBox use_location;
    private boolean if_use_location;
    private Button btn_best_road;
    private Button btn_short_road;
    private Button btn_help;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        use_location = (CheckBox) findViewById(R.id.use_location);
        btn_best_road=  (Button) findViewById(R.id.btn_best_road);
        btn_short_road = (Button) findViewById(R.id.btn_short_road);
        btn_help = (Button) findViewById(R.id.btn_help);

        btn_best_road.setOnClickListener(this);
        btn_short_road.setOnClickListener(this);
        btn_help.setOnClickListener(this);

        use_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                if_use_location = use_location.isChecked();
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_best_road:
                Intent intent1=new Intent(this,BestRoadActivity.class);
                intent1.putExtra("if_use_location",if_use_location);
                startActivity(intent1);

                break;
            case R.id.btn_short_road:
                Intent intent2=new Intent(this,ShortRoadActivity.class);
                intent2.putExtra("if_use_location",if_use_location);
                startActivity(intent2);

                break;
            case R.id.btn_help:
                Intent intent3=new Intent(this,HelpActivity.class);
                startActivity(intent3);

                break;
        }
    }
}

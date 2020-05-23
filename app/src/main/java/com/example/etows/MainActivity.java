package com.example.etows;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button nTower, nCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nTower = (Button) findViewById(R.id.tower);
        nCustomer = (Button) findViewById(R.id.customer);

        startService(new Intent(MainActivity.this, OnAppKilled.class));

        nTower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TowerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        nCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CustomerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}

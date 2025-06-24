package com.example.habitwhisper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnXray = findViewById(R.id.btn_xray);
        Button btnVoice = findViewById(R.id.btn_voice);
        Button btnMedicalTerm = findViewById(R.id.btn_medical_term);
        Button btnTablet = findViewById(R.id.btn_tablet);

        btnXray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FeatureXrayActivity.class));
            }
        });
        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FeatureVoiceActivity.class));
            }
        });
        btnMedicalTerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FeatureMedicalTermActivity.class));
            }
        });
        btnTablet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FeatureTabletActivity.class));
            }
        });
    }
}

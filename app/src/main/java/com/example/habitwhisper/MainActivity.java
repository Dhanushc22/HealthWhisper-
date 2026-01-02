package com.example.habitwhisper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize modern card views
        CardView cardXray = findViewById(R.id.card_xray);
        CardView cardVoice = findViewById(R.id.card_voice);
        CardView cardMedicalTerm = findViewById(R.id.card_medical_term);
        CardView cardTablet = findViewById(R.id.card_tablet);

        // Set up modern card click listeners with smooth animations
        cardXray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardClick(v);
                startActivity(new Intent(MainActivity.this, FeatureXrayActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        cardVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardClick(v);
                startActivity(new Intent(MainActivity.this, FeatureVoiceActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        cardMedicalTerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardClick(v);
                startActivity(new Intent(MainActivity.this, FeatureMedicalTermActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        cardTablet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCardClick(v);
                startActivity(new Intent(MainActivity.this, FeatureTabletActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
    }

    private void animateCardClick(View view) {
        // Add subtle scale animation for better user feedback
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100);
                }
            });
    }
}

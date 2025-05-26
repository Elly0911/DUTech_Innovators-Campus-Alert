package com.example.pbdv_project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class ResolvedItemsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolved_items);

        MaterialCardView cardAlerts = findViewById(R.id.cardAlerts);
        MaterialCardView cardReports = findViewById(R.id.cardReports);

        cardAlerts.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResolvedAlertsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        cardReports.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResolvedReportsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}
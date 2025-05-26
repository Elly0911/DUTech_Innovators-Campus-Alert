package com.example.pbdv_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.onesignal.OneSignal;

public class ViewAlertsActivity extends AppCompatActivity {
    private static final String TAG = "ViewAlertsActivity";

    private RecyclerView alertsRecyclerView;
    private AlertAdapter alertAdapter;
    private FirebaseFirestore fStore;
    private ProgressBar progressBar;
    private TextView noAlertsText;
    private EditText searchInput;
    private String targetAlertId = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_alerts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fStore = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        noAlertsText = findViewById(R.id.noAlertsText);
        searchInput = findViewById(R.id.searchInput);

        handleIntent(getIntent());
        setupRecyclerView();
        setupNotificationHandler();
        setupSearch();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                alertAdapter.setSearchQuery(searchText);
                checkEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkEmptyState() {
        if (alertAdapter == null) return;

        boolean isEmpty = alertAdapter.getItemCount() == 0;
        noAlertsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        alertsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (searchInput.getText().toString().isEmpty()) {
                noAlertsText.setText("No alerts found");
            } else {
                noAlertsText.setText("No alerts match your search");
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("alert_id")) {
            targetAlertId = intent.getStringExtra("alert_id");
            Log.d(TAG, "Received target alert ID: " + targetAlertId);
        }
    }

    private void setupRecyclerView() {
        alertsRecyclerView = findViewById(R.id.alertsRecyclerView);
        alertsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar.setVisibility(View.VISIBLE);
        noAlertsText.setVisibility(View.GONE);

        Query query = fStore.collection("alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Alert> options = new FirestoreRecyclerOptions.Builder<Alert>()
                .setQuery(query, snapshot -> {
                    Alert alert = snapshot.toObject(Alert.class);
                    if (alert != null) {
                        alert.setDocumentId(snapshot.getId());
                    }
                    return alert;
                })
                .build();

        alertAdapter = new AlertAdapter(options, this);
        alertsRecyclerView.setAdapter(alertAdapter);

        if (targetAlertId != null) {
            alertAdapter.setTargetAlertId(targetAlertId);
        }

        alertAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                progressBar.setVisibility(View.GONE);
                checkEmptyState();

                if (targetAlertId != null) {
                    scrollToTargetAlert();
                }
            }

            @Override
            public void onChanged() {
                super.onChanged();
                progressBar.setVisibility(View.GONE);
                checkEmptyState();
            }
        });
    }

    private void scrollToTargetAlert() {
        if (targetAlertId == null || alertAdapter == null) return;

        int position = alertAdapter.getPositionForAlertId(targetAlertId);
        if (position >= 0) {
            alertsRecyclerView.smoothScrollToPosition(position);
        }
    }

    private void setupNotificationHandler() {
        OneSignal.getNotifications().addClickListener(result -> {
            String alertId = result.getNotification().getAdditionalData().optString("alert_id", null);
            if (alertId != null) {
                Intent intent = new Intent(this, ViewAlertsActivity.class);
                intent.putExtra("alert_id", alertId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);

        if (alertAdapter != null && targetAlertId != null) {
            alertAdapter.setTargetAlertId(targetAlertId);
            scrollToTargetAlert();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (alertAdapter != null) {
            alertAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alertAdapter != null) {
            alertAdapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertAdapter != null) {
            alertAdapter.stopListening();
        }
    }
}
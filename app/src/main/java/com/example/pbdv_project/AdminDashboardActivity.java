package com.example.pbdv_project;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

        private static final String TAG = "AdminDashboardActivity";
        private FirebaseUser user;
        private FirebaseFirestore fStore;
        private ImageView profileImage;
        private LineChart dailyTrendsChart;
        private PieChart panicAlertsChart;
        private BarChart userDistributionChart;
        private ProgressBar dailyTrendsProgress;
        private ProgressBar panicAlertsProgress;
        private ProgressBar userDistributionProgress;
        private ImageButton refreshButton;
        private MaterialButton viewAlertsButton;
        private MaterialButton viewReportsButton;
        private MaterialButton viewResolvedItemsButton;
        private ListenerRegistration profileImageListener;

        private final ActivityResultLauncher<Intent> profileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                        if (result.getResultCode() == RESULT_OK) {
                                // Always refreshes the profile image when returning from ProfileActivity
                                loadProfileImage();
                        }
                }
        );

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_admin_dashboard);

                profileImage = findViewById(R.id.profileImageCard).findViewById(R.id.profileImage);
                CardView profileImageCard = findViewById(R.id.profileImageCard);
                profileImageCard.setOnClickListener(this::showProfileMenu);

                viewAlertsButton = findViewById(R.id.viewAlertsButton);
                viewReportsButton = findViewById(R.id.viewReportsButton);
                viewResolvedItemsButton = findViewById(R.id.viewResolvedItemsButton);

                viewAlertsButton.setOnClickListener(v -> {
                        startActivity(new Intent(this, ViewPanicAlertsActivity.class));
                });

                viewReportsButton.setOnClickListener(v -> {
                        startActivity(new Intent(this, ViewReportsActivity.class));
                });

                viewResolvedItemsButton.setOnClickListener(v -> {
                    startActivity(new Intent(this, ResolvedItemsActivity.class));
                });

                // Initialize charts
                dailyTrendsChart = findViewById(R.id.dailyTrendsChart);
                panicAlertsChart = findViewById(R.id.panicAlertsChart);
                userDistributionChart = findViewById(R.id.userDistributionChart);

                // Initialize progress bars
                dailyTrendsProgress = findViewById(R.id.dailyTrendsProgress);
                panicAlertsProgress = findViewById(R.id.panicAlertsProgress);
                userDistributionProgress = findViewById(R.id.userDistributionProgress);

                // Initialize refresh button
                refreshButton = findViewById(R.id.refreshButton);
                refreshButton.setOnClickListener(v -> refreshAllData());

                user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                }
                fStore = FirebaseFirestore.getInstance();

                loadProfileImage();
                setupProfileImageListener();
                setupToolbar();

                setupCharts();

                loadAllData();
        }

        private void refreshAllData() {
                Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
                loadAllData();
        }

        private void loadAllData() {
                showLoading();
                loadResolvedTrends();
                loadPanicAlerts();
                loadUserDistribution();
        }

        private void showLoading() {
                dailyTrendsChart.setVisibility(View.GONE);
                panicAlertsChart.setVisibility(View.GONE);
                userDistributionChart.setVisibility(View.GONE);

                dailyTrendsProgress.setVisibility(View.VISIBLE);
                panicAlertsProgress.setVisibility(View.VISIBLE);
                userDistributionProgress.setVisibility(View.VISIBLE);
        }

        private void hideLoading() {}

        private void chartLoadingComplete() {
                // Check if all charts have loaded
                if (dailyTrendsChart.getVisibility() == View.VISIBLE &&
                        panicAlertsChart.getVisibility() == View.VISIBLE &&
                        userDistributionChart.getVisibility() == View.VISIBLE) {

                        findViewById(R.id.toolbar).requestFocus();
                }
        }

        private void showProfileMenu(View anchor) {
                PopupMenu popupMenu = new PopupMenu(this, anchor);
                popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.menu_profile_view) {
                                Intent profileIntent = new Intent(this, ProfileViewActivity.class);
                                profileActivityResultLauncher.launch(profileIntent);
                                return true;
                        } else if (item.getItemId() == R.id.menu_profile_logout) {
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                                return true;
                        }
                        return false;
                });

                popupMenu.show();
        }

        private void setupProfileImageListener() {
                profileImageListener = fStore.collection("users").document(user.getUid())
                        .addSnapshotListener((documentSnapshot, e) -> {
                                if (e != null) {
                                        Log.w(TAG, "Listen failed.", e);
                                        return;
                                }

                                if (documentSnapshot != null && documentSnapshot.exists()) {
                                        String photoUrl = documentSnapshot.getString("photoUrl");
                                        if (photoUrl != null && !photoUrl.isEmpty()) {
                                                Glide.with(this)
                                                        .load(photoUrl)
                                                        .circleCrop()
                                                        .into(profileImage);
                                        } else {
                                                profileImage.setImageResource(R.drawable.ic_person_improved);
                                        }
                                }
                        });
        }

        private void loadProfileImage() {
                if (user.getPhotoUrl() != null) {
                        Glide.with(this)
                                .load(user.getPhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_person_improved)
                                .into(profileImage);
                }
        }

        private void setupToolbar() {
                androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Campus Alert");
                }
        }

        private void setupCharts() {

                dailyTrendsChart.getDescription().setEnabled(false);
                dailyTrendsChart.setDrawGridBackground(false);
                dailyTrendsChart.setTouchEnabled(true);
                dailyTrendsChart.setDragEnabled(true);
                dailyTrendsChart.setScaleEnabled(true);
                dailyTrendsChart.setPinchZoom(true);

                // X-axis styling
                XAxis xAxis = dailyTrendsChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setTextColor(Color.DKGRAY);
                xAxis.setDrawGridLines(false);
                xAxis.setAxisLineColor(Color.GRAY);
                xAxis.setAxisLineWidth(1f);
                xAxis.setGranularity(1f);

                // Y-axis styling
                YAxis leftAxis = dailyTrendsChart.getAxisLeft();
                leftAxis.setTextColor(Color.DKGRAY);
                leftAxis.setAxisLineColor(Color.GRAY);
                leftAxis.setAxisLineWidth(1f);
                leftAxis.setDrawGridLines(true);
                leftAxis.setGridColor(Color.LTGRAY);
                leftAxis.setGranularity(1f);
                leftAxis.setAxisMinimum(0f);

                dailyTrendsChart.getAxisRight().setEnabled(false);

                // Legend styling
                dailyTrendsChart.getLegend().setTextColor(Color.DKGRAY);
                dailyTrendsChart.getLegend().setTextSize(12f);
                dailyTrendsChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                dailyTrendsChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                dailyTrendsChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
                dailyTrendsChart.getLegend().setDrawInside(false);

                dailyTrendsChart.setNoDataText("No trend data available");
                dailyTrendsChart.setNoDataTextColor(Color.GRAY);
                dailyTrendsChart.setExtraBottomOffset(10f);

                panicAlertsChart.getDescription().setEnabled(false);
                panicAlertsChart.setDrawHoleEnabled(true);
                panicAlertsChart.setHoleColor(Color.TRANSPARENT);
                panicAlertsChart.setTransparentCircleRadius(45f);
                panicAlertsChart.setHoleRadius(40f);
                panicAlertsChart.setDrawCenterText(true);
                panicAlertsChart.setRotationEnabled(true);
                panicAlertsChart.setHighlightPerTapEnabled(true);

                panicAlertsChart.getLegend().setTextColor(Color.DKGRAY);
                panicAlertsChart.getLegend().setTextSize(12f);
                panicAlertsChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                panicAlertsChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                panicAlertsChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
                panicAlertsChart.getLegend().setDrawInside(false);

                panicAlertsChart.setNoDataText("No panic alerts today");
                panicAlertsChart.setNoDataTextColor(Color.GRAY);
                panicAlertsChart.setEntryLabelColor(Color.DKGRAY);
                panicAlertsChart.setEntryLabelTextSize(12f);
                panicAlertsChart.setExtraBottomOffset(10f);

                userDistributionChart.getDescription().setEnabled(false);
                userDistributionChart.setDrawGridBackground(false);
                userDistributionChart.setTouchEnabled(true);
                userDistributionChart.setDragEnabled(true);
                userDistributionChart.setScaleEnabled(true);
                userDistributionChart.setPinchZoom(true);

                XAxis xBarAxis = userDistributionChart.getXAxis();
                xBarAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xBarAxis.setDrawGridLines(false);
                xBarAxis.setGranularity(1f);
                xBarAxis.setTextColor(Color.DKGRAY);
                xBarAxis.setAxisLineColor(Color.GRAY);
                xBarAxis.setAxisLineWidth(1f);

                YAxis leftBarAxis = userDistributionChart.getAxisLeft();
                leftBarAxis.setTextColor(Color.DKGRAY);
                leftBarAxis.setAxisLineColor(Color.GRAY);
                leftBarAxis.setAxisLineWidth(1f);
                leftBarAxis.setDrawGridLines(true);
                leftBarAxis.setGridColor(Color.LTGRAY);
                leftBarAxis.setGranularity(1f);
                leftBarAxis.setAxisMinimum(0f);

                userDistributionChart.getAxisRight().setEnabled(false);

                userDistributionChart.getLegend().setTextColor(Color.DKGRAY);
                userDistributionChart.getLegend().setTextSize(12f);
                userDistributionChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                userDistributionChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                userDistributionChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
                userDistributionChart.getLegend().setDrawInside(false);

                userDistributionChart.setNoDataText("No user data available");
                userDistributionChart.setNoDataTextColor(Color.GRAY);
                userDistributionChart.setExtraBottomOffset(10f);
        }

        private void loadResolvedTrends() {
                dailyTrendsProgress.setVisibility(View.VISIBLE);
                dailyTrendsChart.setVisibility(View.GONE);

                // Get the date range (last 7 days)
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -6);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                long startDate = calendar.getTimeInMillis();

                calendar.add(Calendar.DAY_OF_YEAR, 6);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                long endDate = calendar.getTimeInMillis();

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                List<String> dateLabels = new ArrayList<>();
                Calendar tempCal = Calendar.getInstance();
                tempCal.setTimeInMillis(startDate);

                for (int i = 0; i < 7; i++) {
                        dateLabels.add(sdf.format(tempCal.getTime()));
                        tempCal.add(Calendar.DAY_OF_YEAR, 1);
                }

                // Create final arrays for daily counts
                final int[] resolvedAlertsCount = new int[7];
                final int[] resolvedIncidentsCount = new int[7];

                // Query resolved alerts collection (firebase)
                fStore.collection("resolved_alerts")
                        .whereGreaterThanOrEqualTo("resolvedTimestamp", startDate)
                        .whereLessThanOrEqualTo("resolvedTimestamp", endDate)
                        .get()
                        .addOnCompleteListener(alertTask -> {
                                if (alertTask.isSuccessful()) {
                                        for (DocumentSnapshot doc : alertTask.getResult()) {
                                                long timestamp = doc.getLong("resolvedTimestamp");
                                                Date date = new Date(timestamp);
                                                String dayKey = sdf.format(date);

                                                // Find which day index this belongs to
                                                int dayIndex = dateLabels.indexOf(dayKey);
                                                if (dayIndex >= 0) {
                                                        resolvedAlertsCount[dayIndex]++;
                                                }
                                        }

                                        // Query resolved incidents collection (firebase)
                                        fStore.collection("resolved_incidents")
                                                .whereGreaterThanOrEqualTo("resolvedTimestamp", startDate)
                                                .whereLessThanOrEqualTo("resolvedTimestamp", endDate)
                                                .get()
                                                .addOnCompleteListener(incidentTask -> {
                                                        dailyTrendsProgress.setVisibility(View.GONE);
                                                        dailyTrendsChart.setVisibility(View.VISIBLE);

                                                        if (incidentTask.isSuccessful()) {
                                                                for (DocumentSnapshot doc : incidentTask.getResult()) {
                                                                        long timestamp = doc.getLong("resolvedTimestamp");
                                                                        Date date = new Date(timestamp);
                                                                        String dayKey = sdf.format(date);

                                                                        // Find which day index this belongs to
                                                                        int dayIndex = dateLabels.indexOf(dayKey);
                                                                        if (dayIndex >= 0) {
                                                                                resolvedIncidentsCount[dayIndex]++;
                                                                        }
                                                                }

                                                                // Prepare chart data
                                                                List<Entry> alertEntries = new ArrayList<>();
                                                                List<Entry> incidentEntries = new ArrayList<>();

                                                                for (int i = 0; i < dateLabels.size(); i++) {
                                                                        alertEntries.add(new Entry(i, resolvedAlertsCount[i]));
                                                                        incidentEntries.add(new Entry(i, resolvedIncidentsCount[i]));
                                                                }

                                                                LineDataSet alertDataSet = new LineDataSet(alertEntries, "Resolved Panic Alerts");
                                                                alertDataSet.setColor(Color.parseColor("#FF6B6B"));
                                                                alertDataSet.setCircleColor(Color.parseColor("#FF6B6B"));
                                                                alertDataSet.setLineWidth(2.5f);
                                                                alertDataSet.setCircleRadius(5f);
                                                                alertDataSet.setValueTextSize(11f);
                                                                alertDataSet.setValueTextColor(Color.DKGRAY);
                                                                alertDataSet.setMode(LineDataSet.Mode.LINEAR);
                                                                alertDataSet.setDrawFilled(true);
                                                                alertDataSet.setFillColor(Color.parseColor("#30FF6B6B"));
                                                                alertDataSet.setFillAlpha(100);

                                                                LineDataSet incidentDataSet = new LineDataSet(incidentEntries, "Resolved Incidents");
                                                                incidentDataSet.setColor(Color.parseColor("#4ECDC4"));
                                                                incidentDataSet.setCircleColor(Color.parseColor("#4ECDC4"));
                                                                incidentDataSet.setLineWidth(2.5f);
                                                                incidentDataSet.setCircleRadius(5f);
                                                                incidentDataSet.setValueTextSize(11f);
                                                                incidentDataSet.setValueTextColor(Color.DKGRAY);
                                                                incidentDataSet.setMode(LineDataSet.Mode.LINEAR);
                                                                incidentDataSet.setDrawFilled(true);
                                                                incidentDataSet.setFillColor(Color.parseColor("#304ECDC4"));
                                                                incidentDataSet.setFillAlpha(100);

                                                                LineData data = new LineData(alertDataSet, incidentDataSet);
                                                                dailyTrendsChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dateLabels));
                                                                dailyTrendsChart.setData(data);
                                                                dailyTrendsChart.invalidate();
                                                                dailyTrendsChart.animateY(1000);
                                                        } else {
                                                                Toast.makeText(this, "Failed to load resolved incidents", Toast.LENGTH_SHORT).show();
                                                        }
                                                        chartLoadingComplete();
                                                });
                                } else {
                                        dailyTrendsProgress.setVisibility(View.GONE);
                                        dailyTrendsChart.setVisibility(View.VISIBLE);
                                        Toast.makeText(this, "Failed to load resolved alerts", Toast.LENGTH_SHORT).show();
                                        chartLoadingComplete();
                                }
                        });
        }

        private void loadPanicAlerts() {
                panicAlertsProgress.setVisibility(View.VISIBLE);
                panicAlertsChart.setVisibility(View.GONE);

                Log.d(TAG, "Loading panic alerts");

                // Get today's date range
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDay = calendar.getTimeInMillis();

                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                long endOfDay = calendar.getTimeInMillis();

                Log.d(TAG, "Today's date range: " + new Date(startOfDay) + " to " + new Date(endOfDay));

                final int[] queryCompletionCounter = {0};
                final int[] activeCount = {0};
                final int[] resolvedCount = {0};

                // Query active panic alerts - query all active alerts first
                fStore.collection("panic_alerts")
                        .whereEqualTo("status", "active")
                        .get()
                        .addOnSuccessListener(activeSnapshots -> {
                                // First log all active panic alerts for debugging
                                Log.d(TAG, "All active panic alerts count: " + activeSnapshots.size());

                                // Filter for today's alerts manually
                                int todaysActiveCount = 0;
                                for (DocumentSnapshot doc : activeSnapshots) {
                                        Long timestamp = doc.getLong("timestamp");
                                        if (timestamp != null) {
                                                Log.d(TAG, "Alert ID: " + doc.getId() +
                                                        ", timestamp: " + timestamp +
                                                        ", date: " + new Date(timestamp));

                                                if (timestamp >= startOfDay && timestamp <= endOfDay) {
                                                        todaysActiveCount++;
                                                        Log.d(TAG, "This alert is from today");
                                                } else {
                                                        Log.d(TAG, "This alert is NOT from today");
                                                }
                                        } else {
                                                Log.w(TAG, "Alert ID: " + doc.getId() + " has no timestamp field");
                                        }
                                }

                                activeCount[0] = todaysActiveCount;
                                Log.d(TAG, "Today's active panic alerts: " + activeCount[0]);

                                // Increment counter and check if both queries are done
                                queryCompletionCounter[0]++;
                                if (queryCompletionCounter[0] == 2) {
                                        updatePieChartWithData(activeCount[0], resolvedCount[0]);
                                }
                        })
                        .addOnFailureListener(e -> {
                                Log.e(TAG, "Error querying active panic alerts", e);
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Failed to load active panic alerts", Toast.LENGTH_SHORT).show();

                                queryCompletionCounter[0]++;
                                if (queryCompletionCounter[0] == 2) {
                                        updatePieChartWithData(activeCount[0], resolvedCount[0]);
                                }
                        });

                // Query resolved panic alerts collection (only for today)
                fStore.collection("resolved_alerts")
                        .whereGreaterThanOrEqualTo("resolvedTimestamp", startOfDay)
                        .whereLessThanOrEqualTo("resolvedTimestamp", endOfDay)
                        .get()
                        .addOnSuccessListener(resolvedSnapshots -> {
                                resolvedCount[0] = resolvedSnapshots.size();
                                Log.d(TAG, "Today's resolved panic alerts: " + resolvedCount[0]);

                                // Increment counter and check if both queries are done
                                queryCompletionCounter[0]++;
                                if (queryCompletionCounter[0] == 2) {
                                        updatePieChartWithData(activeCount[0], resolvedCount[0]);
                                }
                        })
                        .addOnFailureListener(e -> {
                                Log.e(TAG, "Error querying resolved panic alerts", e);
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Failed to load resolved panic alerts", Toast.LENGTH_SHORT).show();

                                queryCompletionCounter[0]++;
                                if (queryCompletionCounter[0] == 2) {
                                        updatePieChartWithData(activeCount[0], resolvedCount[0]);
                                }
                        });
        }

        private void updatePieChartWithData(int activeCount, int resolvedCount) {
                panicAlertsProgress.setVisibility(View.GONE);
                panicAlertsChart.setVisibility(View.VISIBLE);

                List<PieEntry> entries = new ArrayList<>();
                int total = activeCount + resolvedCount;

                if (total > 0) {
                        if (activeCount > 0) {
                                entries.add(new PieEntry(activeCount, "Active"));
                        }
                        if (resolvedCount > 0) {
                                entries.add(new PieEntry(resolvedCount, "Resolved"));
                        }

                        PieDataSet dataSet = new PieDataSet(entries, "Panic Alerts Status");
                        dataSet.setColors(new int[]{Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4")});
                        dataSet.setValueTextColor(Color.DKGRAY);
                        dataSet.setValueTextSize(12f);
                        dataSet.setValueLinePart1OffsetPercentage(80f);
                        dataSet.setValueLinePart1Length(0.3f);
                        dataSet.setValueLinePart2Length(0.4f);
                        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                        dataSet.setSliceSpace(2f);
                        dataSet.setSelectionShift(5f);

                        PieData data = new PieData(dataSet);
                        panicAlertsChart.setData(data);
                        panicAlertsChart.setCenterText("Total: " + total);
                        panicAlertsChart.setCenterTextSize(14f);
                        panicAlertsChart.setCenterTextColor(Color.DKGRAY);
                        panicAlertsChart.setEntryLabelColor(Color.DKGRAY);
                        panicAlertsChart.setEntryLabelTextSize(12f);
                } else {
                        panicAlertsChart.clear();
                        panicAlertsChart.setCenterText("No panic alerts today");
                        panicAlertsChart.setCenterTextSize(14f);
                        panicAlertsChart.setCenterTextColor(Color.GRAY);
                }

                panicAlertsChart.invalidate();
                panicAlertsChart.animateY(1000);

                Log.d(TAG, "Pie chart updated with active: " + activeCount + ", resolved: " + resolvedCount);
                chartLoadingComplete();
        }

        private void loadUserDistribution() {
                userDistributionProgress.setVisibility(View.VISIBLE);
                userDistributionChart.setVisibility(View.GONE);

                fStore.collection("users")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                userDistributionProgress.setVisibility(View.GONE);
                                userDistributionChart.setVisibility(View.VISIBLE);

                                int studentCount = 0;
                                int staffCount = 0;
                                int securityCount = 0;

                                Log.d(TAG, "Total users found: " + queryDocumentSnapshots.size());

                                for (DocumentSnapshot document : queryDocumentSnapshots) {
                                        String role = document.getString("role");
                                        if (role != null) {
                                                Log.d(TAG, "User role: " + role);
                                                switch (role.toLowerCase()) {
                                                        case "student":
                                                                studentCount++;
                                                                break;
                                                        case "staff":
                                                                staffCount++;
                                                                break;
                                                        case "security":
                                                                securityCount++;
                                                                break;
                                                        default:
                                                                Log.w(TAG, "Unknown role: " + role);
                                                                break;
                                                }
                                        } else {
                                                Log.w(TAG, "User document has no role field");
                                        }
                                }

                                Log.d(TAG, String.format("Counts - Students: %d, Staff: %d, Security: %d",
                                        studentCount, staffCount, securityCount));

                                updateUserDistributionChart(studentCount, staffCount, securityCount);
                        })
                        .addOnFailureListener(e -> {
                                userDistributionProgress.setVisibility(View.GONE);
                                userDistributionChart.setVisibility(View.VISIBLE);

                                Log.e(TAG, "Error loading user distribution", e);
                                Toast.makeText(this, "Failed to load user distribution", Toast.LENGTH_SHORT).show();
                                updateUserDistributionChart(0, 0, 0);
                        });
        }

        private void updateUserDistributionChart(int studentCount, int staffCount, int securityCount) {
                ArrayList<BarEntry> entries = new ArrayList<>();
                entries.add(new BarEntry(0, studentCount));
                entries.add(new BarEntry(1, staffCount));
                entries.add(new BarEntry(2, securityCount));

                BarDataSet dataSet = new BarDataSet(entries, "User Distribution");
                dataSet.setColors(new int[]{
                        Color.parseColor("#4ECDC4"),  // Light teal for students
                        Color.parseColor("#FFD166"),  // Yellow for staff
                        Color.parseColor("#FF6B6B")   // Coral red for security
                });
                dataSet.setValueTextColor(Color.DKGRAY);
                dataSet.setValueTextSize(12f);
                dataSet.setHighLightColor(Color.parseColor("#EF476F"));
                dataSet.setHighlightEnabled(true);

                ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                dataSets.add(dataSet);

                BarData data = new BarData(dataSets);
                data.setBarWidth(0.6f);
                data.setValueFormatter(new LargeValueFormatter());

                userDistributionChart.setData(data);

                // Set X-axis labels
                ArrayList<String> labels = new ArrayList<>();
                labels.add("Students");
                labels.add("Staff");
                labels.add("Security");

                XAxis xAxis = userDistributionChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setLabelCount(labels.size());
                xAxis.setLabelRotationAngle(-45);

                int totalUsers = studentCount + staffCount + securityCount;
                userDistributionChart.getDescription().setText("Total Users: " + totalUsers);
                userDistributionChart.invalidate();
                userDistributionChart.animateY(1000);

                if (totalUsers == 0) {
                        Toast.makeText(this, "No user data found in Firestore", Toast.LENGTH_LONG).show();
                }

                chartLoadingComplete();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                getMenuInflater().inflate(R.menu.profile_menu, menu);
                return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.menu_profile_view) {
                        startActivity(new Intent(this, ProfileViewActivity.class));
                        return true;
                } else if (id == R.id.menu_profile_logout) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return true;
                }

                return super.onOptionsItemSelected(item);
        }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (profileImageListener != null) {
                        profileImageListener.remove();
                }
        }
}
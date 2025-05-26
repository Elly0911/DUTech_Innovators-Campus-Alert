package com.example.pbdv_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ViewPanicAlertsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ViewPanicAlerts";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final LatLng DEFAULT_LOCATION = new LatLng(0, 0);
    private static final float DEFAULT_ZOOM = 2f;
    private static final float MARKER_ZOOM = 16f;

    private RecyclerView alertsRecyclerView;
    private PanicAlertAdapter adapter;
    private FirebaseFirestore fStore;
    private TextView noAlertsText;
    private GoogleMap googleMap;
    private boolean mapReady = false;
    private boolean isShowingAlert = false;
    private ListenerRegistration alertCountListener;
    private String lastSelectedAlertId = null;
    private boolean initialLoadComplete = false;
    private boolean isDataLoading = true;

    // Store pending alert ID to handle when adapter is ready
    private String pendingAlertId = null;
    private int maxRetryAttempts = 10;
    private int currentRetryCount = 0;

    private static final int SECURITY_LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final float SECURITY_LOCATION_CIRCLE_RADIUS = 20f; // meters
    private Circle securityLocationCircle;
    private Handler securityLocationHandler;
    private Runnable securityLocationRunnable;
    private LatLng lastKnownSecurityLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_panic_alerts);

        initialLoadComplete = false;
        isDataLoading = true;

        fStore = FirebaseFirestore.getInstance();
        alertsRecyclerView = findViewById(R.id.alertsRecyclerView);
        noAlertsText = findViewById(R.id.noAlertsText);

        updateInitialUIState();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupRecyclerView();
        setupAlertCountListener();
        setupNotificationHandler();

        handleIntentNotification(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            isDataLoading = false;
            updateEmptyState();

            checkPendingAlertSelection();
        }
    }

    private void updateInitialUIState() {
        noAlertsText.setVisibility(View.GONE);
        alertsRecyclerView.setVisibility(View.GONE);
        findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.addressText).setVisibility(View.GONE);
        findViewById(R.id.mapLoadingProgress).setVisibility(View.VISIBLE);
    }

    private void setupSecurityLocationUpdates() {
        securityLocationHandler = new Handler(Looper.getMainLooper());
        securityLocationRunnable = new Runnable() {
            @Override
            public void run() {
                updateSecurityLocation();
                securityLocationHandler.postDelayed(this, SECURITY_LOCATION_UPDATE_INTERVAL);
            }
        };
        securityLocationHandler.post(securityLocationRunnable);
    }

    private void updateSecurityLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation == null) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null) {
                LatLng newLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                updateSecurityLocationOnMap(newLocation);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting security location", e);
        }
    }

    private void updateSecurityLocationOnMap(LatLng location) {
        if (googleMap == null) return;

        lastKnownSecurityLocation = location;

        if (securityLocationCircle == null) {
            securityLocationCircle = googleMap.addCircle(new CircleOptions()
                    .center(location)
                    .radius(SECURITY_LOCATION_CIRCLE_RADIUS)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(Color.argb(100, 0, 0, 255))
                    .zIndex(1));
        } else {
            securityLocationCircle.setCenter(location);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        mapReady = true;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);
            setupSecurityLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        if (!isDataLoading && adapter != null) {
            updateEmptyState();
            checkPendingAlertSelection();
        }
    }

    private void setupAlertCountListener() {
        alertCountListener = fStore.collection("panic_alerts")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d(TAG, "Active alerts count: " + snapshots.size());
                        if (!isDataLoading) {
                            updateEmptyState();
                        }
                    } else {
                        Log.d(TAG, "No active alerts found");
                        if (!isDataLoading) {
                            updateEmptyState();
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        Query query = fStore.collection("panic_alerts")
                .whereEqualTo("status", "active")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);

        FirestoreRecyclerOptions<PanicAlert> options = new FirestoreRecyclerOptions.Builder<PanicAlert>()
                .setQuery(query, this::convertSnapshotToAlert)
                .setLifecycleOwner(this)
                .build();

        adapter = new PanicAlertAdapter(options, new PanicAlertAdapter.OnAlertClickListener() {
            @Override
            public void onAlertClick(PanicAlert alert) {
                handleAlertSelection(alert);
            }

            @Override
            public void onResolveClick(PanicAlert alert, int position) {
                resolveAlert(alert, position);
            }

            @Override
            public void onDataLoaded(boolean hasData) {
                isDataLoading = false;
                runOnUiThread(() -> {
                    findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
                    noAlertsText.setVisibility(hasData ? View.GONE : View.VISIBLE);
                    alertsRecyclerView.setVisibility(hasData ? View.VISIBLE : View.GONE);

                    if (!hasData && googleMap != null) {
                        googleMap.clear();
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                        findViewById(R.id.addressText).setVisibility(View.GONE);
                    }

                    checkPendingAlertSelection();
                });
            }
        }, true);

        alertsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alertsRecyclerView.setAdapter(adapter);
        initialLoadComplete = true;
    }

    private PanicAlert convertSnapshotToAlert(DocumentSnapshot snapshot) {
        PanicAlert alert = snapshot.toObject(PanicAlert.class);
        if (alert == null) {
            alert = new PanicAlert();
        }
        alert.setDocumentId(snapshot.getId());

        alert.setUserName(snapshot.getString("userName") != null ?
                snapshot.getString("userName") : "Unknown User");
        alert.setStatus(snapshot.getString("status") != null ?
                snapshot.getString("status") : "active");
        alert.setUserPhone(snapshot.getString("userPhone"));
        alert.setUserEmail(snapshot.getString("userEmail"));

        List<Map<String, String>> contacts = (List<Map<String, String>>) snapshot.get("emergencyContactsList");
        alert.setEmergencyContactsList(contacts);

        Long timestamp = snapshot.getLong("timestamp");
        alert.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());

        Double lat = snapshot.getDouble("latitude");
        Double lng = snapshot.getDouble("longitude");
        if (lat != null) alert.setLatitude(lat);
        if (lng != null) alert.setLongitude(lng);

        alert.setUserId(snapshot.getString("userId"));
        alert.setCategory(snapshot.getString("category"));

        return alert;
    }

    private void updateEmptyState() {
        runOnUiThread(() -> {
            if (adapter == null) return;

            boolean isEmpty = adapter.getItemCount() == 0;
            Log.d(TAG, "updateEmptyState: isEmpty=" + isEmpty + ", isDataLoading=" + isDataLoading);

            findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
            noAlertsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            alertsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (isEmpty) {
                if (googleMap != null) {
                    googleMap.clear();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                }
                findViewById(R.id.addressText).setVisibility(View.GONE);
                isShowingAlert = false;
                lastSelectedAlertId = null;
            }
        });
    }

    private void handleAlertSelection(PanicAlert alert) {
        if (alert == null) {
            Toast.makeText(this, "Invalid alert data", Toast.LENGTH_SHORT).show();
            return;
        }

        isShowingAlert = true;
        lastSelectedAlertId = alert.getDocumentId();

        runOnUiThread(() -> {
            findViewById(R.id.addressText).setVisibility(View.GONE);
            findViewById(R.id.mapLoadingProgress).setVisibility(View.VISIBLE);
            noAlertsText.setVisibility(View.GONE);
            alertsRecyclerView.setVisibility(View.VISIBLE);
        });

        if (!mapReady || googleMap == null || (alert.getLatitude() == 0 && alert.getLongitude() == 0)) {
            resetAlertState();
            return;
        }

        LatLng location = new LatLng(alert.getLatitude(), alert.getLongitude());
        updateMapWithLocation(location, alert.getUserName(), alert.getEmergencyContactsList());
    }

    private void resetAlertState() {
        isShowingAlert = false;
        lastSelectedAlertId = null;
        runOnUiThread(() -> {
            findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
            updateEmptyState();
        });
    }

    private void updateMapWithLocation(LatLng location, String userName, List<Map<String, String>> emergencyContacts) {
        runOnUiThread(() -> {
            try {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(userName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                final LatLng finalLocation = location;
                final String finalUserName = userName;
                final List<Map<String, String>> finalEmergencyContacts = emergencyContacts;

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalLocation, MARKER_ZOOM),
                        new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                handleMapAnimationComplete(finalLocation, finalUserName, finalEmergencyContacts);
                            }

                            @Override
                            public void onCancel() {
                                handleMapAnimationComplete(finalLocation, finalUserName, finalEmergencyContacts);
                            }
                        });
            } catch (Exception e) {
                resetAlertState();
                Log.e(TAG, "Error updating map", e);
            }
        });
    }

    private void handleMapAnimationComplete(LatLng location, String userName,
                                            List<Map<String, String>> emergencyContacts) {
        final LatLng finalLocation = location;
        final String finalUserName = userName;
        final List<Map<String, String>> finalEmergencyContacts = emergencyContacts;

        runOnUiThread(() -> {
            findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
            findViewById(R.id.addressText).setVisibility(View.VISIBLE);
            showAddress(finalLocation, finalUserName, finalEmergencyContacts);
        });
    }

    private void showAddress(LatLng location, String userName, List<Map<String, String>> emergencyContacts) {
        final LatLng finalLocation = location;
        final String finalUserName = userName;
        final List<Map<String, String>> finalEmergencyContacts = emergencyContacts;

        new Thread(() -> {
            try {
                String finalAddress = getFinalAddressFromLocation(finalLocation);
                StringBuilder contactsInfo = buildEmergencyContactsInfo(finalEmergencyContacts);

                runOnUiThread(() -> {
                    TextView addressView = findViewById(R.id.addressText);
                    addressView.setText(String.format("%s is at:\n%s%s", finalUserName, finalAddress, contactsInfo));
                });
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error", e);
                runOnUiThread(() -> {
                    TextView addressView = findViewById(R.id.addressText);
                    addressView.setText(String.format("Location: %s, %s", finalLocation.latitude, finalLocation.longitude));
                });
            }
        }).start();
    }

    private String getFinalAddressFromLocation(LatLng location) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(
                location.latitude, location.longitude, 1);

        StringBuilder addressText = new StringBuilder();
        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            if (address.getThoroughfare() != null) {
                addressText.append(address.getThoroughfare());
            }
            if (address.getLocality() != null) {
                if (addressText.length() > 0) addressText.append(", ");
                addressText.append(address.getLocality());
            }
            if (address.getCountryName() != null) {
                if (addressText.length() > 0) addressText.append(", ");
                addressText.append(address.getCountryName());
            }
        }

        return addressText.length() > 0 ?
                addressText.toString() :
                "Location: " + location.latitude + ", " + location.longitude;
    }

    private StringBuilder buildEmergencyContactsInfo(List<Map<String, String>> emergencyContacts) {
        StringBuilder contactsInfo = new StringBuilder();
        if (emergencyContacts != null && !emergencyContacts.isEmpty()) {
            contactsInfo.append("\n\nEmergency Contacts:\n");
            for (Map<String, String> contact : emergencyContacts) {
                String name = contact.get("name");
                String relationship = contact.get("relationship");
                String phone = contact.get("phone");

                contactsInfo.append("• ")
                        .append(name != null ? name : "Unknown")
                        .append(" (")
                        .append(relationship != null ? relationship : "No relationship specified")
                        .append("): ")
                        .append(phone != null ? phone : "No phone number")
                        .append("\n");
            }
        }
        return contactsInfo;
    }

    private void resolveAlert(PanicAlert alert, int position) {
        if (alert == null || alert.getDocumentId() == null) {
            Toast.makeText(this, "Invalid alert", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser.getUid();
        String currentUserName = currentUser.getDisplayName(); // Get the user's name

        findViewById(R.id.mapLoadingProgress).setVisibility(View.VISIBLE);

        final String alertId = alert.getDocumentId();
        final String alertUserId = alert.getUserId();
        final boolean isDisplayedAlert = lastSelectedAlertId != null &&
                lastSelectedAlertId.equals(alertId);

        adapter.removeAlert(position);

        if (adapter.getItemCount() == 0) {
            updateEmptyState();
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "resolved");
        updates.put("resolvedTimestamp", System.currentTimeMillis());
        updates.put("resolvedBy", currentUserName);

        fStore.collection("panic_alerts").document(alertId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Then create a copy in resolved_alerts
                    Map<String, Object> resolvedAlert = new HashMap<>(alert.toMap());
                    resolvedAlert.put("resolvedTimestamp", System.currentTimeMillis());
                    resolvedAlert.put("status", "resolved");
                    resolvedAlert.put("resolvedBy", currentUserName);

                    if (!resolvedAlert.containsKey("category") || resolvedAlert.get("category") == null) {
                        resolvedAlert.put("category", "Panic Alert");
                    }

                    fStore.collection("resolved_alerts").add(resolvedAlert)
                            .addOnSuccessListener(documentReference -> {
                                findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
                                Toast.makeText(ViewPanicAlertsActivity.this,
                                        "Alert resolved and archived", Toast.LENGTH_SHORT).show();

                                if (alertUserId != null) {
                                    notifyStudentAlertResolved(alertUserId);
                                }

                                if (isDisplayedAlert) {
                                    if (googleMap != null) {
                                        googleMap.clear();
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
                                    }
                                    findViewById(R.id.addressText).setVisibility(View.GONE);
                                    isShowingAlert = false;
                                    lastSelectedAlertId = null;
                                    updateEmptyState();
                                }
                            })
                            .addOnFailureListener(e -> {
                                findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
                                Log.e(TAG, "Error archiving resolved alert", e);
                                Toast.makeText(ViewPanicAlertsActivity.this,
                                        "Failed to archive alert", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.mapLoadingProgress).setVisibility(View.GONE);
                    Toast.makeText(ViewPanicAlertsActivity.this,
                            "Failed to resolve alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error resolving alert", e);
                });
    }

    private void notifyStudentAlertResolved(String userId) {
        if (userId == null || userId.isEmpty()) return;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                JSONObject additionalData = new JSONObject()
                        .put("type", "alert_resolved")
                        .put("user_id", userId)
                        .put("action", "alert_resolved");

                JSONObject filter = new JSONObject()
                        .put("field", "tag")
                        .put("key", "user_id")
                        .put("relation", "=")
                        .put("value", userId);

                JSONObject payload = new JSONObject()
                        .put("app_id", ApplicationClass.ONESIGNAL_APP_ID)
                        .put("filters", new JSONArray().put(filter))
                        .put("contents", new JSONObject().put("en", "Your emergency alert has been resolved by security"))
                        .put("headings", new JSONObject().put("en", "Alert Resolved"))
                        .put("data", additionalData)
                        .put("small_icon", "ic_stat_onesignal_default")
                        .put("large_icon", "ic_launcher_foreground")
                        .put("android_accent_color", "FF00FF00")
                        .put("android_visibility", 1)
                        .put("priority", 6)
                        .put("adm_small_icon", "ic_launcher");

                Request request = new Request.Builder()
                        .url("https://onesignal.com/api/v1/notifications")
                        .addHeader("Authorization", "Basic " + ApplicationClass.ONESIGNAL_REST_API_KEY)
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Resolution notification sent successfully: " + responseBody);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Failed to send resolution notification", e);
                runOnUiThread(() ->
                        Toast.makeText(ViewPanicAlertsActivity.this,
                                "Failed to send resolution notification", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupNotificationHandler() {
        OneSignal.getNotifications().addClickListener(result -> {
            try {
                JSONObject additionalData = result.getNotification().getAdditionalData();
                if (additionalData != null) {
                    String alertId = additionalData.optString("alert_id", null);
                    String type = additionalData.optString("type", null);

                    if (alertId != null && "panic_alert".equals(type)) {
                        Log.d(TAG, "Notification clicked with alertId: " + alertId);
                        processPendingAlert(alertId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling notification click", e);
            }
        });
    }

    private void handleIntentNotification(Intent intent) {
        if (intent == null) return;

        try {
            String onesignalData = intent.getStringExtra("onesignal_data");
            if (onesignalData != null) {
                JSONObject data = new JSONObject(onesignalData);
                String alertId = data.optString("alert_id", null);
                String type = data.optString("type", null);

                if (alertId != null && "panic_alert".equals(type)) {
                    Log.d(TAG, "Found alert_id in onesignal_data: " + alertId);
                    processPendingAlert(alertId);
                    return;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing onesignal_data", e);
        }

        if (intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                Object value = intent.getExtras().get(key);
                Log.d(TAG, "Intent Extra: " + key + " = " + value);

                if (value instanceof String && ((String) value).contains("alert_id")) {
                    try {
                        JSONObject jsonData = new JSONObject((String) value);
                        String alertId = jsonData.optString("alert_id", null);
                        if (alertId != null) {
                            Log.d(TAG, "Found alert_id in extra " + key + ": " + alertId);
                            processPendingAlert(alertId);
                            return;
                        }
                    } catch (JSONException ignored) {}
                }
            }
        }

        if (intent.hasExtra("alert_id")) {
            String alertId = intent.getStringExtra("alert_id");
            String type = intent.getStringExtra("type");

            if (alertId != null && (type == null || "panic_alert".equals(type))) {
                Log.d(TAG, "Found direct alert_id in intent: " + alertId);
                processPendingAlert(alertId);
            }
        }
    }

    private void processPendingAlert(String alertId) {
        if (alertId == null || alertId.isEmpty()) return;

        pendingAlertId = alertId;
        currentRetryCount = 0;
        Log.d(TAG, "Setting pending alert ID: " + alertId);

        checkPendingAlertSelection();
    }

    private void checkPendingAlertSelection() {
        if (pendingAlertId == null || adapter == null || isDataLoading) {
            return;
        }

        Log.d(TAG, "Checking for pending alert: " + pendingAlertId + ", retry: " + currentRetryCount);

        PanicAlert foundAlert = null;
        int position = -1;

        for (int i = 0; i < adapter.getItemCount(); i++) {
            PanicAlert alert = adapter.getItem(i);
            if (alert != null && pendingAlertId.equals(alert.getDocumentId())) {
                foundAlert = alert;
                position = i;
                break;
            }
        }

        if (foundAlert != null) {
            Log.d(TAG, "Found pending alert at position: " + position);
            pendingAlertId = null;
            currentRetryCount = 0;

            final int finalPosition = position;
            final PanicAlert finalAlert = foundAlert;

            new Handler(Looper.getMainLooper()).post(() -> {
                alertsRecyclerView.smoothScrollToPosition(finalPosition);
                handleAlertSelection(finalAlert);
            });
        } else {
            currentRetryCount++;
            if (currentRetryCount < maxRetryAttempts) {
                Log.d(TAG, "Alert not found, scheduling retry " + currentRetryCount);
                new Handler(Looper.getMainLooper()).postDelayed(
                        this::checkPendingAlertSelection, 1000);
            } else {
                Log.d(TAG, "Max retries reached, giving up on finding alert: " + pendingAlertId);
                pendingAlertId = null;
                currentRetryCount = 0;
                Toast.makeText(this, "Could not find the requested alert", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentNotification(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }

        if (alertCountListener != null) {
            alertCountListener.remove();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopListening();
        }

        if (alertCountListener != null) {
            alertCountListener.remove();
        }

        if (securityLocationHandler != null && securityLocationRunnable != null) {
            securityLocationHandler.removeCallbacks(securityLocationRunnable);
        }

        googleMap = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && googleMap != null) {
                try {
                    googleMap.setMyLocationEnabled(true);
                    setupSecurityLocationUpdates();
                } catch (SecurityException e) {
                    Log.e(TAG, "Location permission exception", e);
                }
            }
        }
    }
}
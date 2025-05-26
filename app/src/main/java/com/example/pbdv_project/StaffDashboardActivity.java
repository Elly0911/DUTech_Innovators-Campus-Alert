package com.example.pbdv_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.firebase.firestore.ListenerRegistration;
import static com.example.pbdv_project.Constants.*;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StaffDashboardActivity extends AppCompatActivity {
    private static final String TAG = "StaffDashboard";
    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private TextView welcomeText;
    private View panicButton;
    private ImageView profileImage;
    private static final int REQUEST_CHECK_SETTINGS = 1002;
    private ListenerRegistration profileImageListener;
    private final ActivityResultLauncher<Intent> profileActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadProfileImage();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_staff_dashboard);

        profileImage = findViewById(R.id.profileImageCard).findViewById(R.id.profileImage);
        CardView profileImageCard = findViewById(R.id.profileImageCard);

        profileImageCard.setOnClickListener(this::showProfileMenu);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        fStore = FirebaseFirestore.getInstance();
        welcomeText = findViewById(R.id.welcomeText);

        panicButton = findViewById(R.id.panicButton);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Campus Alert");
        }

        loadProfileImage();
        setupProfileImageListener();
        setUserNameInWelcome();
        setupClickListeners();
        checkLocationPermission();
        checkBatteryOptimization();
        enableGPS();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void loadProfileImage() {
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_improved)
                    .into(profileImage);
        } else {
            fStore.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_person_improved)
                                        .into(profileImage);
                            }
                        }
                    });
        }
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

    private void updateProfileImage(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_improved)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_person_improved);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setUserNameInWelcome() {
        String displayName = user.getDisplayName();

        if (displayName != null && !displayName.isEmpty()) {
            welcomeText.setText("Welcome, " + displayName + "!");
        } else {
            fStore.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String name = task.getResult().getString("name");
                            if (name != null && !name.isEmpty()) {
                                welcomeText.setText("Welcome, " + name + "!");
                            } else {
                                welcomeText.setText("Welcome to Campus Alert!");
                            }
                        } else {
                            welcomeText.setText("Welcome to Campus Alert!");
                        }
                    });
        }
    }

    private void setupClickListeners() {
        panicButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LocationHelper.LOCATION_PERMISSION_REQUEST_CODE);
                Toast.makeText(this, "Please grant location permission", Toast.LENGTH_SHORT).show();
                return;
            }
            showPanicConfirmationDialog();
        });

        findViewById(R.id.reportCard).setOnClickListener(v ->
                startActivity(new Intent(this, ReportIncidentActivity.class)));
        findViewById(R.id.alertsCard).setOnClickListener(v ->
                startActivity(new Intent(this, ViewAlertsActivity.class)));
        findViewById(R.id.contactsCard).setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyContactsActivity.class)));
        findViewById(R.id.tipsCard).setOnClickListener(v ->
                startActivity(new Intent(this, SafetyTipsActivity.class)));
    }

    private void showPanicConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Activate Emergency Panic Alert")
                .setMessage("This will send your location and emergency contacts to security team immediately."
                        + "\n"
                        + "Please use only in genuine emergency situations.")
                .setPositiveButton("SEND ALERT", (dialog, which) -> triggerPanicAlert())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void triggerPanicAlert() {
        panicButton.setEnabled(false);

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Getting your precise location...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        fStore.collection("users").document(user.getUid())
                .collection("emergency_contacts").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> contactsList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, String> contact = new HashMap<>();
                        contact.put("name", document.getString("name"));
                        contact.put("phone", document.getString("phone"));
                        contact.put("relationship", document.getString("relationship"));
                        contactsList.add(contact);
                    }
                    getLocationWithTimeout(contactsList, progressDialog);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    getLocationWithTimeout(new ArrayList<>(), progressDialog);
                });
    }

    private void getLocationWithTimeout(List<Map<String, String>> emergencyContacts, AlertDialog progressDialog) {
        final Handler handler = new Handler();
        final Runnable timeoutRunnable = () -> {
            progressDialog.dismiss();
            sendPanicAlert(0, 0, emergencyContacts);
            Toast.makeText(StaffDashboardActivity.this,
                    "Using last known location as precise location couldn't be obtained quickly",
                    Toast.LENGTH_LONG).show();
        };

        handler.postDelayed(timeoutRunnable, 15000);

        LocationHelper locationHelper = new LocationHelper(this);
        locationHelper.getCurrentLocation(new LocationHelper.MyLocationCallback() {
            @Override
            public void onLocationResult(Location location) {
                handler.removeCallbacks(timeoutRunnable);
                progressDialog.dismiss();
                if (location != null) {
                    sendPanicAlert(location.getLatitude(), location.getLongitude(), emergencyContacts);
                } else {
                    locationHelper.getLastLocation().addOnSuccessListener(lastLocation -> {
                        if (lastLocation != null) {
                            sendPanicAlert(lastLocation.getLatitude(), lastLocation.getLongitude(), emergencyContacts);
                        } else {
                            sendPanicAlert(0, 0, emergencyContacts);
                        }
                    });
                }
            }

            @Override
            public void onLocationError(String error) {
                handler.removeCallbacks(timeoutRunnable);
                progressDialog.dismiss();
                Toast.makeText(StaffDashboardActivity.this,
                        "Location error: " + error + ". Sending alert with last known location.",
                        Toast.LENGTH_LONG).show();

                locationHelper.getLastLocation().addOnSuccessListener(lastLocation -> {
                    if (lastLocation != null) {
                        sendPanicAlert(lastLocation.getLatitude(), lastLocation.getLongitude(), emergencyContacts);
                    } else {
                        sendPanicAlert(0, 0, emergencyContacts);
                    }
                });
            }
        });
    }

    private void sendPanicAlert(double latitude, double longitude, List<Map<String, String>> emergencyContacts) {
        fStore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userPhone = documentSnapshot.getString("phone");

                        PanicAlert alert = new PanicAlert();
                        alert.setUserId(user.getUid());
                        alert.setUserName(user.getDisplayName());
                        alert.setUserEmail(user.getEmail());
                        alert.setUserPhone(userPhone);
                        alert.setLatitude(latitude);
                        alert.setLongitude(longitude);
                        alert.setTimestamp(System.currentTimeMillis());
                        alert.setStatus(ACTIVE_STATUS);
                        alert.setEmergencyContactsList(emergencyContacts);
                        alert.setCategory(DEFAULT_ALERT_CATEGORY);

                        // Get address from location
                        new Thread(() -> {
                            try {
                                Geocoder geocoder = new Geocoder(StaffDashboardActivity.this, Locale.getDefault());
                                List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    android.location.Address address = addresses.get(0);
                                    StringBuilder addressText = new StringBuilder();
                                    if (address.getThoroughfare() != null) addressText.append(address.getThoroughfare());
                                    if (address.getLocality() != null) {
                                        if (addressText.length() > 0) addressText.append(", ");
                                        addressText.append(address.getLocality());
                                    }
                                    if (address.getCountryName() != null) {
                                        if (addressText.length() > 0) addressText.append(", ");
                                        addressText.append(address.getCountryName());
                                    }
                                    alert.setAddress(addressText.toString());
                                }
                            } catch (IOException e) {
                                Log.e("Dashboard", "Geocoder error", e);
                            }

                            runOnUiThread(() -> {
                                fStore.collection("panic_alerts").add(alert.toMap())
                                        .addOnSuccessListener(documentReference -> {
                                            panicButton.setEnabled(true);
                                            Toast.makeText(StaffDashboardActivity.this,
                                                    "Panic alert sent with your location", Toast.LENGTH_LONG).show();

                                            // Send OneSignal notification to security
                                            sendPanicAlertNotification(documentReference.getId(), alert);
                                        })
                                        .addOnFailureListener(e -> {
                                            panicButton.setEnabled(true);
                                            Toast.makeText(StaffDashboardActivity.this,
                                                    "Failed to send alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            });
                        }).start();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show();
                    panicButton.setEnabled(true);
                });
    }

    private void sendPanicAlertNotification(String alertId, PanicAlert alert) {
        new Thread(() -> {
            try {
                String locationText = alert.getAddress() != null ?
                        alert.getAddress() :
                        String.format(Locale.getDefault(), "%.6f, %.6f", alert.getLatitude(), alert.getLongitude());

                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                // Create notification data with deep link
                JSONObject additionalData = new JSONObject()
                        .put("alert_id", alertId)
                        .put("type", "panic_alert")
                        .put("action", "view_panic_alert")
                        .put("onesignal_data", new JSONObject()
                                .put("alert_id", alertId)
                                .put("type", "panic_alert")
                                .toString());

                // Target only security staff
                JSONObject filter = new JSONObject()
                        .put("field", "tag")
                        .put("key", "user_role")
                        .put("relation", "=")
                        .put("value", "Security");

                String notificationTitle = "Emergency Alert!";
                String notificationMessage = String.format("%s needs help at %s",
                        alert.getUserName() != null ? alert.getUserName() : "A user",
                        locationText);

                JSONObject payload = new JSONObject()
                        .put("app_id", ApplicationClass.ONESIGNAL_APP_ID)
                        .put("filters", new JSONArray().put(filter))
                        .put("contents", new JSONObject().put("en", notificationMessage))
                        .put("headings", new JSONObject().put("en", notificationTitle))
                        .put("data", additionalData)
                        .put("small_icon", "ic_stat_onesignal_default")
                        .put("large_icon", "ic_launcher_foreground")
                        .put("android_accent_color", "FFFF0000") // Red for emergency
                        .put("adm_small_icon", "ic_launcher");

                Request request = new Request.Builder()
                        .url("https://onesignal.com/api/v1/notifications")
                        .addHeader("Authorization", "Basic " + ApplicationClass.ONESIGNAL_REST_API_KEY)
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                Log.d(TAG, "Notification sent: " + response.body().string());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Failed to send panic alert notification", e);
            }
        }).start();
    }


    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LocationHelper.LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableGPS() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, response -> {});

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {}
            }
        });
    }

    private void checkBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isPowerSaveMode()) {
            new AlertDialog.Builder(this)
                    .setTitle("Battery Saver Mode")
                    .setMessage("Battery saver mode may affect location accuracy. For best results, disable battery optimization for this app.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Location settings enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location settings not enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
            Intent profileIntent = new Intent(this, ProfileViewActivity.class);
            startActivity(profileIntent);
            return true;
        } else if (id == R.id.menu_profile_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
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
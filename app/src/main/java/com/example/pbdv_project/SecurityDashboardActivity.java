package com.example.pbdv_project;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Objects;

public class SecurityDashboardActivity extends AppCompatActivity {

    private FirebaseUser user;
    private TextView welcomeText;
    private CardView cardSendAlert, cardViewReports, cardViewPanicAlerts, cardViewResolvedItems;
    private TextView panicAlertBadge, reportBadge;
    private FirebaseFirestore fStore;
    private ListenerRegistration panicAlertsListener, reportsListener;
    private ImageView refreshButton;
    private CardView refreshCard;
    private ImageView profileImage;
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
        setContentView(R.layout.activity_security_dashboard);

        initializeViews();
        setupAuth();
        setupToolbar();
        setupClickListeners();
        setupPanicAlertListener();
        setupReportsListener();
        updateWelcomeText();
        loadProfileImage();
        setupProfileImageListener();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        CardView profileImageCard = findViewById(R.id.profileImageCard);

        welcomeText = findViewById(R.id.welcomeText);
        cardSendAlert = findViewById(R.id.cardSendAlert);
        cardViewReports = findViewById(R.id.cardViewReports);
        cardViewPanicAlerts = findViewById(R.id.cardViewPanicAlerts);
        cardViewResolvedItems = findViewById(R.id.cardViewResolvedItems);
        panicAlertBadge = findViewById(R.id.panicAlertBadge);
        reportBadge = findViewById(R.id.reportBadge);
        refreshButton = findViewById(R.id.refreshButton);
        refreshCard = findViewById(R.id.refreshCard);

        profileImageCard.setOnClickListener(this::showProfileMenu);
    }

    private void setupAuth() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadProfileImage();
        fStore = FirebaseFirestore.getInstance();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Campus Alert");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateWelcomeText() {
        if (user != null && user.getDisplayName() != null) {
            welcomeText.setText("Welcome, " + user.getDisplayName());
        } else {
            welcomeText.setText("Welcome to Security Dashboard");
        }
    }

    private void setupClickListeners() {
        cardSendAlert.setOnClickListener(v -> startActivity(new Intent(SecurityDashboardActivity.this, SendAlertActivity.class)));
        cardViewReports.setOnClickListener(v -> startActivity(new Intent(SecurityDashboardActivity.this, ViewReportsActivity.class)));
        cardViewPanicAlerts.setOnClickListener(v -> startActivity(new Intent(SecurityDashboardActivity.this, ViewPanicAlertsActivity.class)));
        cardViewResolvedItems.setOnClickListener(v -> startActivity(new Intent(SecurityDashboardActivity.this, ResolvedItemsActivity.class)));
        refreshButton.setOnClickListener(v -> performRefresh());
        refreshCard.setOnClickListener(v -> performRefresh());
    }

    private void performRefresh() {
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(1000);
        refreshButton.startAnimation(rotate);

        checkForNewAlerts();
        checkForNewReports();
        Toast.makeText(this, "Checking for new alerts...", Toast.LENGTH_SHORT).show();
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
        try {
            if (user != null && user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person_improved)
                        .into(profileImage);
            } else if (user != null) {
                fStore.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String photoUrl = documentSnapshot.getString("photoUrl");
                                if (photoUrl != null && !photoUrl.isEmpty()) {
                                    Glide.with(this)
                                            .load(photoUrl)
                                            .circleCrop()
                                            .into(profileImage);
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.e("SecurityDashboard", "Error loading profile image", e));
            }
        } catch (Exception e) {
            Log.e("SecurityDashboard", "Error in loadProfileImage", e);
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
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_person_improved);
        }
    }

    private void setupPanicAlertListener() {
        try {
            Query query = fStore.collection("panic_alerts")
                    .whereEqualTo("status", "active");

            panicAlertsListener = query.addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.w("Dashboard", "Listen failed.", error);
                    return;
                }

                try {
                    int count = value != null ? value.size() : 0;
                    updatePanicAlertBadge(count);
                } catch (Exception e) {
                    Log.e("Dashboard", "Error processing alerts", e);
                }
            });
        } catch (Exception e) {
            Log.e("Dashboard", "Error setting up listener", e);
        }
    }

    private void setupReportsListener() {
        try {
            Query query = fStore.collection("incidents")
                    .whereEqualTo("status", "pending");

            reportsListener = query.addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.w("Dashboard", "Listen failed.", error);
                    return;
                }

                try {
                    int count = value != null ? value.size() : 0;
                    updateReportBadge(count);
                } catch (Exception e) {
                    Log.e("Dashboard", "Error processing reports", e);
                }
            });
        } catch (Exception e) {
            Log.e("Dashboard", "Error setting up reports listener", e);
        }
    }

    private void checkForNewAlerts() {
        fStore.collection("panic_alerts")
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult() != null ? task.getResult().size() : 0;
                        updatePanicAlertBadge(count);
                    } else {
                        Log.w("Dashboard", "Error getting alerts", task.getException());
                    }
                });
    }

    private void checkForNewReports() {
        fStore.collection("incidents")
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult() != null ? task.getResult().size() : 0;
                        updateReportBadge(count);
                    } else {
                        Log.w("Dashboard", "Error getting reports", task.getException());
                    }
                });
    }

    private void updatePanicAlertBadge(int count) {
        runOnUiThread(() -> {
            if (count > 0) {
                panicAlertBadge.setText(String.valueOf(count));
                panicAlertBadge.setVisibility(View.VISIBLE);

                panicAlertBadge.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .withEndAction(() -> panicAlertBadge.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200))
                        .start();
            } else {
                panicAlertBadge.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void updateReportBadge(int count) {
        runOnUiThread(() -> {
            if (count > 0) {
                reportBadge.setText(String.valueOf(count));
                reportBadge.setVisibility(View.VISIBLE);

                reportBadge.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(200)
                        .withEndAction(() -> reportBadge.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200))
                        .start();
            } else {
                reportBadge.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileImageListener != null) {
            profileImageListener.remove();
        }
        if (panicAlertsListener != null) {
            panicAlertsListener.remove();
        }
        if (reportsListener != null) {
            reportsListener.remove();
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
}
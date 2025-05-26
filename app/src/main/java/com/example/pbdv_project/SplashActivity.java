package com.example.pbdv_project;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.Continue;
import com.onesignal.OneSignal;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 4000;
    private static final long MIN_SPLASH_TIME = 3200;
    private long startTime;
    private boolean isNavigating = false;
    private FirebaseUser preloadedUser;
    private String preloadedRole;
    private boolean dataPreloaded = false;
    private Handler handler;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.SplashTheme);
        startTime = System.currentTimeMillis();
        setContentView(R.layout.activity_splash);

        handler = new Handler(Looper.getMainLooper());

        CardView splashCard = findViewById(R.id.splash_card);
        ImageView logoImage = findViewById(R.id.splash_logo);
        TextView appTitle = findViewById(R.id.app_title_text);
        TextView tagline = findViewById(R.id.tagline_text);
        View accentLine = findViewById(R.id.accent_line);
        TextView versionText = findViewById(R.id.version_text);
        TextView copyrightText = findViewById(R.id.copyright_text);
        ProgressBar loadingIndicator = findViewById(R.id.loading_indicator);

        splashCard.setAlpha(0f);
        logoImage.setScaleX(0.5f);
        logoImage.setScaleY(0.5f);
        appTitle.setAlpha(0f);
        tagline.setAlpha(0f);
        accentLine.setScaleX(0f);
        versionText.setAlpha(0f);
        copyrightText.setAlpha(0f);
        loadingIndicator.setAlpha(0f);

        setupAnimations(splashCard, logoImage, appTitle, tagline, accentLine, versionText, copyrightText, loadingIndicator);

        requestNotificationPermission();

        preloadUserData();

        handler.postDelayed(this::checkUserAndNavigate, SPLASH_DELAY);
    }

    private void setupAnimations(CardView splashCard, ImageView logoImage, TextView appTitle,
                                 TextView tagline, View accentLine, TextView versionText,
                                 TextView copyrightText, ProgressBar loadingIndicator) {
        // Card animation
        ObjectAnimator cardFadeIn = ObjectAnimator.ofFloat(splashCard, "alpha", 0f, 1f);
        ObjectAnimator cardMoveUp = ObjectAnimator.ofFloat(splashCard, "translationY", 50f, 0f);

        // Logo animation
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.5f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.5f, 1f);

        // Text animations
        ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(appTitle, "alpha", 0f, 1f);
        ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        ObjectAnimator accentLineScale = ObjectAnimator.ofFloat(accentLine, "scaleX", 0f, 1f);
        ObjectAnimator versionFadeIn = ObjectAnimator.ofFloat(versionText, "alpha", 0f, 1f);
        ObjectAnimator copyrightFadeIn = ObjectAnimator.ofFloat(copyrightText, "alpha", 0f, 1f);
        ObjectAnimator loaderFadeIn = ObjectAnimator.ofFloat(loadingIndicator, "alpha", 0f, 1f);

        AnimatorSet cardAnimSet = new AnimatorSet();
        cardAnimSet.playTogether(cardFadeIn, cardMoveUp);
        cardAnimSet.setDuration(1200); // Increased from 900
        cardAnimSet.setInterpolator(new DecelerateInterpolator());

        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoScaleX, logoScaleY);
        logoAnimSet.setDuration(1000);
        logoAnimSet.setStartDelay(600);
        logoAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet textAnimSet = new AnimatorSet();
        textAnimSet.playSequentially(
                titleFadeIn.setDuration(400),
                taglineFadeIn.setDuration(400),
                accentLineScale.setDuration(400),
                versionFadeIn.setDuration(400)
        );
        textAnimSet.setStartDelay(1000);
        textAnimSet.setDuration(400);

        AnimatorSet fullAnimSet = new AnimatorSet();
        fullAnimSet.playTogether(cardAnimSet, logoAnimSet, textAnimSet,
                copyrightFadeIn.setDuration(800),
                loaderFadeIn.setDuration(800));
        fullAnimSet.start();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityResultLauncher<String> requestPermissionLauncher =
                        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                            if (isGranted) {
                                Log.d(TAG, "Notification permission granted");
                            } else {
                                Log.d(TAG, "Notification permission denied");
                            }
                        });
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void preloadUserData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        preloadedUser = auth.getCurrentUser();

        if (preloadedUser != null) {
            FirebaseFirestore fStore = FirebaseFirestore.getInstance();
            fStore.collection("users").document(preloadedUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            preloadedRole = documentSnapshot.getString("role");
                        }
                        dataPreloaded = true;
                        ensureMinimumSplashTime();
                    })
                    .addOnFailureListener(e -> {
                        dataPreloaded = true;
                        ensureMinimumSplashTime();
                    });
        } else {
            dataPreloaded = true;
            ensureMinimumSplashTime();
        }
    }

    private void ensureMinimumSplashTime() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime < MIN_SPLASH_TIME) {
            handler.postDelayed(this::checkUserAndNavigate, MIN_SPLASH_TIME - elapsedTime);
        } else {
            checkUserAndNavigate();
        }
    }

    private void checkUserAndNavigate() {
        if (isNavigating) return;

        if (dataPreloaded) {
            navigateToDestination();
        } else {
            handler.postDelayed(this::navigateToDestination, 500);
        }
    }

    private void navigateToDestination() {
        if (isNavigating) return;
        isNavigating = true;

        Intent intent;

        if (preloadedUser == null) {
            intent = new Intent(this, LoginActivity.class);
        } else if ("Admin".equals(preloadedRole)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if ("Security".equals(preloadedRole)) {
            intent = new Intent(this, SecurityDashboardActivity.class);
        } else {
            intent = new Intent(this, StudentDashboardActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        handler.postDelayed(() -> {
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 300);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {}

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNavigating) {
            isNavigating = false;
            handler.postDelayed(this::checkUserAndNavigate, 1000);
        }
    }
}
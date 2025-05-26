// ApplicationClass.java
package com.example.pbdv_project;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.user.state.IUserStateObserver;
import com.onesignal.user.state.UserState;

public class ApplicationClass extends Application {
    public static final String TAG = "ApplicationClass";
    public static final String ONESIGNAL_APP_ID = "Your_app_id";
    public static final String ONESIGNAL_REST_API_KEY = "Your_api_key";

    @Override
    public void onCreate() {
        super.onCreate();

        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // Initialize OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // Sync Firebase user roles with OneSignal tags
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                String userId = firebaseAuth.getCurrentUser().getUid();

                // Fetch user role from Firestore
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String role = documentSnapshot.getString("role"); // "staff", "student", "security"
                            if (role != null) {
                                // Tag the user in OneSignal
                                OneSignal.login(userId); // Link Firebase UID to OneSignal
                                OneSignal.getUser().addTag("user_role", role);
                                OneSignal.getUser().addTag("user_id", userId);

                                Log.d(TAG, "User tagged with role: " + role);
                            }
                        });
            } else {
                OneSignal.logout();
            }
        });



        // Request notification permission
        OneSignal.getNotifications().requestPermission(true, Continue.none());
    }
}
package com.example.pbdv_project;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendAlertActivity extends AppCompatActivity {
    private static final String TAG = "SendAlertActivity";
    private EditText alertTitleEditText, alertMessageEditText;
    private Spinner alertTypeSpinner;
    private Button sendAlertButton;
    private FirebaseFirestore fStore;
    private String selectedAlertType = "";
    private List<String> alertTypesWithEmojis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_alert);

        fStore = FirebaseFirestore.getInstance();
        alertTypeSpinner = findViewById(R.id.alertTypeSpinner);
        alertTitleEditText = findViewById(R.id.alertTitleEditText);
        alertMessageEditText = findViewById(R.id.alertMessageEditText);
        sendAlertButton = findViewById(R.id.sendAlertButton);

        String[] alertTypes = getResources().getStringArray(R.array.alert_types);
        for (String type : alertTypes) {
            alertTypesWithEmojis.add(getEmojiForAlertType(type) + type);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                alertTypesWithEmojis
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alertTypeSpinner.setAdapter(adapter);

        alertTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = alertTypes[position];
                selectedAlertType = selected;
                if (selected.equals("Other (specify below)")) {
                    alertTitleEditText.setVisibility(View.VISIBLE);
                    alertTitleEditText.setText("");
                } else if (!selected.equals("Select an alert type")) {
                    alertTitleEditText.setVisibility(View.GONE);
                } else {
                    alertTitleEditText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sendAlertButton.setOnClickListener(v -> sendAlert());
    }

    private void sendAlert() {
        String message = alertMessageEditText.getText().toString().trim();
        String title;

        if (selectedAlertType.equals("Select an alert type")) {
            Toast.makeText(this, "Please select an alert type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAlertType.equals("Other (specify below)")) {
            title = alertTitleEditText.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a custom alert title", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            title = selectedAlertType;
        }

        if (message.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String emojiTitle = getEmojiForAlertType(title) + title;
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", emojiTitle);
        alert.put("message", message);
        alert.put("timestamp", Timestamp.now());
        alert.put("read", false);
        alert.put("type", "security_alert");
        alert.put("senderId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        fStore.collection("alerts")
                .add(alert)
                .addOnSuccessListener(documentReference -> {
                    // Send push notification to staff and students
                    sendPushNotification(emojiTitle, message, documentReference.getId());
                    Toast.makeText(this, "Alert sent successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send alert", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving alert", e);
                });
    }

    private String getEmojiForAlertType(String alertType) {
        switch (alertType) {
            case "Fire":
                return "🔥 ";
            case "Lockdown":
                return "🔒 ";
            case "Bomb Threat":
                return "💣 ";
            case "Electrical Fault":
                return "⚡ ";
            case "Power Failure":
                return "🔌 ";
            case "Flooding":
                return "🌊 ";
            case "Other (specify below)":
                return "⚠️ ";
            case "Select an alert type":
                return "";
            default:
                return "⚠️ "; // Default alert icon
        }
    }

    private void sendPushNotification(String title, String message, String alertId) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                JSONObject additionalData = new JSONObject()
                        .put("alert_id", alertId);

                // Target staff and students (exclude security)
                JSONArray filters = new JSONArray()
                        .put(new JSONObject().put("field", "tag").put("key", "user_role").put("relation", "=").put("value", "Staff"))
                        .put(new JSONObject().put("operator", "OR"))
                        .put(new JSONObject().put("field", "tag").put("key", "user_role").put("relation", "=").put("value", "Student"));

                JSONObject payload = new JSONObject()
                        .put("app_id", ApplicationClass.ONESIGNAL_APP_ID)
                        .put("filters", filters)
                        .put("contents", new JSONObject().put("en", message))
                        .put("headings", new JSONObject().put("en", title))
                        .put("data", additionalData)
                        .put("small_icon", "ic_stat_onesignal_default")
                        .put("large_icon", "ic_launcher_foreground")
                        .put("android_accent_color", "FF2196F3")
                        .put("adm_small_icon", "ic_launcher");

                Request request = new Request.Builder()
                        .url("https://onesignal.com/api/v1/notifications")
                        .addHeader("Authorization", "Basic " + ApplicationClass.ONESIGNAL_REST_API_KEY)
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                Log.d(TAG, "Notification sent: " + response.body().string());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Failed to send push notification", e);
            }
        }).start();
    }
}
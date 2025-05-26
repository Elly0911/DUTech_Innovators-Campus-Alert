package com.example.pbdv_project;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.onesignal.OneSignal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewReportsActivity extends AppCompatActivity {
    private static final String TAG = "ViewReportsActivity";
    private RecyclerView reportsRecyclerView;
    private IncidentAdapter adapter;
    private FirebaseFirestore fStore;
    private TextView noReportsText;
    private ProgressDialog progressDialog;
    private Query query;
    private ListenerRegistration reportCountListener;
    private ExecutorService executorService;
    private AlertDialog currentMediaDialog = null;
    private boolean isVideoPreloading = false;
    private String targetIncidentId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reports);

        executorService = Executors.newSingleThreadExecutor();
        fStore = FirebaseFirestore.getInstance();
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        noReportsText = findViewById(R.id.noReportsText);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading reports...");
        progressDialog.setCancelable(false);

        handleIntent(getIntent());
        setupNotificationHandler();
        setupRecyclerView();

        setupReportCountListener();
    }

    private void setupReportCountListener() {
        reportCountListener = fStore.collection("incidents")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        int count = queryDocumentSnapshots.size();
                        updateEmptyState(count == 0);
                    }
                });
    }

    private void updateEmptyState(boolean isEmpty) {
        runOnUiThread(() -> {
            noReportsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            reportsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            // Handle deep link
            String path = intent.getData().getPath();
            if (path != null && path.startsWith("/incidents/")) {
                targetIncidentId = path.substring("/incidents/".length());
            }
        } else if (intent != null && intent.hasExtra("incident_id")) {
            targetIncidentId = intent.getStringExtra("incident_id");
        }
    }

    private void setupNotificationHandler() {
        OneSignal.getNotifications().addClickListener(result -> {
            String incidentId = result.getNotification().getAdditionalData().optString("incident_id", null);
            String type = result.getNotification().getAdditionalData().optString("type", null);

            if (incidentId != null && "new_incident".equals(type)) {
                Intent intent = new Intent(this, ViewReportsActivity.class);
                intent.putExtra("incident_id", incidentId);
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

        if (adapter != null && targetIncidentId != null) {
            adapter.setTargetIncidentId(targetIncidentId);
            scrollToTargetIncident();
        }
    }

    private void setupRecyclerView() {
        query = fStore.collection("incidents")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Incident> options = new FirestoreRecyclerOptions.Builder<Incident>()
                .setQuery(query, snapshot -> {
                    Incident incident = snapshot.toObject(Incident.class);
                    if (incident != null) {
                        incident.setDocumentId(snapshot.getId());
                    }
                    return incident;
                })
                .build();

        adapter = new IncidentAdapter(options, new IncidentAdapter.OnIncidentClickListener() {
            @Override
            public void onIncidentClick(Incident incident) {
                showFullScreenMedia(incident);
            }

            @Override
            public void onResolveClick(Incident incident, int position) {
                resolveIncident(incident, position);
            }

            @Override
            public void onDataLoaded(boolean hasData) {
                updateEmptyState(!hasData);
            }
        });

        if (targetIncidentId != null) {
            adapter.setTargetIncidentId(targetIncidentId);
        }

        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void scrollToTargetIncident() {
        if (targetIncidentId == null || adapter == null) return;

        for (int i = 0; i < adapter.getItemCount(); i++) {
            Incident incident = adapter.getItem(i);
            if (incident != null && targetIncidentId.equals(incident.getDocumentId())) {
                reportsRecyclerView.smoothScrollToPosition(i);
                break;
            }
        }
    }

    private void showFullScreenMedia(Incident incident) {
        if (!incident.isHasMedia()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_media_preview, null);
        builder.setView(dialogView);

        TextView tvMediaType = dialogView.findViewById(R.id.tvMediaType);
        PhotoView dialogImage = dialogView.findViewById(R.id.dialogImage);
        VideoView dialogVideo = dialogView.findViewById(R.id.dialogVideo);

        View loadingOverlay = getLayoutInflater().inflate(R.layout.loading_overlay, null);
        ((ViewGroup) dialogView).addView(loadingOverlay);
        loadingOverlay.setVisibility(View.VISIBLE);

        AlertDialog dialog = builder.create();
        currentMediaDialog = dialog;

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close", (dialogInterface, which) -> {
            if (dialogVideo.isPlaying()) {
                dialogVideo.stopPlayback();
            }
            isVideoPreloading = false;
            dialog.dismiss();
        });

        if (incident.getMediaType().equals("image")) {
            tvMediaType.setText("Photo Evidence");
            dialogImage.setVisibility(View.VISIBLE);
            dialogVideo.setVisibility(View.GONE);

            Glide.with(this)
                    .load(incident.getMediaUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_broken_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(dialogImage);

            dialogImage.setZoomable(true);
            loadingOverlay.setVisibility(View.GONE);
        } else {
            tvMediaType.setText("Video Evidence");
            dialogImage.setVisibility(View.GONE);
            dialogVideo.setVisibility(View.VISIBLE);

            isVideoPreloading = true;
            executorService.execute(() -> {
                try {
                    MediaController mediaController = new MediaController(this);
                    mediaController.setAnchorView(dialogVideo);

                    runOnUiThread(() -> {
                        if (!isVideoPreloading) return;

                        dialogVideo.setMediaController(mediaController);
                        dialogVideo.setVideoURI(Uri.parse(incident.getMediaUrl()));

                        dialogVideo.setOnPreparedListener(mp -> {
                            mp.setOnInfoListener((player, what, extra) -> {
                                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                    loadingOverlay.setVisibility(View.GONE);
                                    return true;
                                }
                                return false;
                            });
                            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                            mp.start();
                        });

                        dialogVideo.setOnErrorListener((mp, what, extra) -> {
                            loadingOverlay.setVisibility(View.GONE);
                            Toast.makeText(ViewReportsActivity.this, "Failed to load video", Toast.LENGTH_SHORT).show();
                            return true;
                        });

                        dialogVideo.requestFocus();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error preloading video", e);
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(ViewReportsActivity.this, "Failed to load video", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        dialogView.setOnClickListener(v -> {
            if (dialogVideo.isPlaying()) {
                dialogVideo.stopPlayback();
            }
            isVideoPreloading = false;
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    private void resolveIncident(Incident incident, int position) {
        progressDialog.show();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser.getUid();
        String currentUserName = currentUser.getDisplayName();

        final String incidentId = incident.getDocumentId();
        final String userId = incident.getUserId();

        adapter.removeIncident(position);

        if (adapter.getItemCount() == 0) {
            updateEmptyState(true);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "resolved");
        updates.put("resolvedTimestamp", System.currentTimeMillis());
        updates.put("resolvedBy", currentUserName);

        // Create a local copy of the incident for the resolved_incidents collection
        Map<String, Object> resolvedIncident = new HashMap<>(incident.toMap());
        resolvedIncident.put("resolvedTimestamp", System.currentTimeMillis());
        resolvedIncident.put("status", "resolved");
        resolvedIncident.put("resolvedBy", currentUserName);

        // First, update the original incident
        fStore.collection("incidents").document(incidentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Then add to resolved_incidents collection
                    fStore.collection("resolved_incidents").add(resolvedIncident)
                            .addOnSuccessListener(documentReference -> {
                                progressDialog.dismiss();
                                Toast.makeText(ViewReportsActivity.this,
                                        "Incident resolved and archived", Toast.LENGTH_SHORT).show();
                                if (userId != null) {
                                    notifyUserIncidentResolved(userId);
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Log.e(TAG, "Error archiving resolved incident", e);
                                Toast.makeText(ViewReportsActivity.this,
                                        "Failed to archive incident", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error resolving incident", e);
                    Toast.makeText(ViewReportsActivity.this,
                            "Failed to resolve incident: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void notifyUserIncidentResolved(String userId) {
        if (userId == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "incident_resolved");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("message", "Your reported incident has been resolved");

        fStore.collection("users").document(userId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Notification sent to user"))
                .addOnFailureListener(e -> Log.e(TAG, "Error sending notification to user", e));
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopListening();
        }
        if (reportCountListener != null) {
            reportCountListener.remove();
        }
        if (currentMediaDialog != null && currentMediaDialog.isShowing()) {
            currentMediaDialog.dismiss();
        }
        isVideoPreloading = false;
        executorService.shutdown();
    }
}
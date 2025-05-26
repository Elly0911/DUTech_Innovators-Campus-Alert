package com.example.pbdv_project;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class ReportIncidentActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_VIDEO_CAPTURE = 2;
    private static final int REQUEST_MEDIA_GALLERY = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 102;

    private static final int VIDEO_QUALITY = 1;
    private static final int VIDEO_DURATION_LIMIT = 30000;
    private static final int PHOTO_QUALITY = 70;

    private EditText etDescription, etLocation;
    private Spinner incidentTypeSpinner;
    private Button btnSubmit, btnTakePhoto, btnTakeVideo, btnChooseMedia;
    private CheckBox cbAnonymous;
    private MaterialCardView mediaCardView;
    private PhotoView ivMediaPreview;
    private VideoView vvMediaPreview;
    private TextView tvMediaType;
    private Button btnRemoveMedia;

    private FirebaseFirestore fStore;
    private FirebaseAuth fAuth;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private Bitmap photoBitmap;
    private Uri mediaUri;
    private String mediaType = null;
    private ProgressDialog progressDialog;
    private String currentPhotoPath;
    private String currentVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_incident);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = fAuth.getCurrentUser();

        initializeViews();
        setupSpinner();
    }

    private void initializeViews() {
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        incidentTypeSpinner = findViewById(R.id.incidentTypeSpinner);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnTakeVideo = findViewById(R.id.btnTakeVideo);
        btnChooseMedia = findViewById(R.id.btnChooseMedia);
        cbAnonymous = findViewById(R.id.cbAnonymous);
        mediaCardView = findViewById(R.id.mediaCardView);
        ivMediaPreview = findViewById(R.id.ivMediaPreview);
        vvMediaPreview = findViewById(R.id.vvMediaPreview);
        tvMediaType = findViewById(R.id.tvMediaType);
        btnRemoveMedia = findViewById(R.id.btnRemoveMedia);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Processing...");

        setupListeners();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.incident_types, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        incidentTypeSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> confirmAndSubmitReport());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        btnTakeVideo.setOnClickListener(v -> checkCameraPermissionForVideo());
        btnChooseMedia.setOnClickListener(v -> checkStoragePermission());
        btnRemoveMedia.setOnClickListener(v -> removeMedia());

        cbAnonymous.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showAnonymousConfirmationDialog();
            }
        });
    }

    private void showAnonymousConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Anonymous Reporting")
                .setMessage("You are about to submit this report anonymously. This means:\n\n" +
                        "• Your personal details will not be stored\n" +
                        "• The report cannot be traced back to you\n\n" +
                        "Do you want to continue with anonymous reporting?")
                .setPositiveButton("Continue Anonymously", (dialog, which) -> {
                    cbAnonymous.setChecked(true);
                    Toast.makeText(this, "Report will be submitted anonymously", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> cbAnonymous.setChecked(false))
                .setCancelable(false)
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void checkCameraPermissionForVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakeVideoIntent();
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                chooseMediaFromGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                chooseMediaFromGallery();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    dispatchTakePictureIntent();
                } else if (permissions[0].equals(Manifest.permission.RECORD_AUDIO)) {
                    dispatchTakeVideoIntent();
                }
            } else {
                Toast.makeText(this, "Camera permission is required to take photos/videos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseMediaFromGallery();
            } else {
                Toast.makeText(this, "Storage permission is required to select media", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if (photoFile != null) {
            mediaUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            File imageFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            currentPhotoPath = imageFile.getAbsolutePath();
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            File videoFile = createVideoFile();
            if (videoFile != null) {
                mediaUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaUri);
                takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, VIDEO_QUALITY);
                takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, VIDEO_DURATION_LIMIT);
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        }
    }

    private File createVideoFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "VID_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        try {
            File videoFile = File.createTempFile(
                    videoFileName,
                    ".mp4",
                    storageDir
            );
            currentVideoPath = videoFile.getAbsolutePath();
            return videoFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void chooseMediaFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(Intent.createChooser(intent, "Select Media"), REQUEST_MEDIA_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            mediaCardView.setVisibility(View.VISIBLE);

            try {
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    mediaType = "image";
                    tvMediaType.setText("Photo Evidence");
                    ivMediaPreview.setVisibility(View.VISIBLE);
                    vvMediaPreview.setVisibility(View.GONE);

                    RequestOptions requestOptions = new RequestOptions()
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_broken_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(800, 600);

                    Glide.with(this)
                            .load(mediaUri)
                            .apply(requestOptions)
                            .into(ivMediaPreview);

                } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
                    if (mediaUri == null && data != null && data.getData() != null) {
                        mediaUri = data.getData();
                    }

                    if (mediaUri != null) {
                        mediaType = "video";
                        tvMediaType.setText("Video Evidence");

                        // Use Glide to create a video thumbnail
                        ivMediaPreview.setVisibility(View.VISIBLE);
                        vvMediaPreview.setVisibility(View.GONE);

                        RequestOptions requestOptions = new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_broken_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .frame(1000) // Capture frame at 1 second
                                .override(800, 600);

                        Glide.with(this)
                                .load(mediaUri)
                                .apply(requestOptions)
                                .into(ivMediaPreview);
                    }
                } else if (requestCode == REQUEST_MEDIA_GALLERY && data != null) {
                    mediaUri = data.getData();
                    String mimeType = getContentResolver().getType(mediaUri);

                    if (mimeType != null && mimeType.contains("video")) {
                        mediaType = "video";
                        tvMediaType.setText("Video Evidence");

                        ivMediaPreview.setVisibility(View.VISIBLE);
                        vvMediaPreview.setVisibility(View.GONE);

                        RequestOptions requestOptions = new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_broken_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .frame(1000)
                                .override(800, 600);

                        Glide.with(this)
                                .load(mediaUri)
                                .apply(requestOptions)
                                .into(ivMediaPreview);

                    } else {
                        mediaType = "image";
                        tvMediaType.setText("Photo Evidence");

                        ivMediaPreview.setVisibility(View.VISIBLE);
                        vvMediaPreview.setVisibility(View.GONE);

                        RequestOptions requestOptions = new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_broken_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .override(800, 600);

                        Glide.with(this)
                                .load(mediaUri)
                                .apply(requestOptions)
                                .into(ivMediaPreview);
                    }
                }

                mediaCardView.setOnClickListener(v -> previewMedia());
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load media: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
    private void previewMedia() {
        if (mediaType == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_media_preview, null);
        builder.setView(dialogView);

        TextView tvMediaType = dialogView.findViewById(R.id.tvMediaType);
        PhotoView dialogImage = dialogView.findViewById(R.id.dialogImage);
        VideoView dialogVideo = dialogView.findViewById(R.id.dialogVideo);

        AlertDialog dialog = builder.create();

        // Add close button to the dialog
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close", (dialogInterface, which) -> {
            if (dialogVideo.isPlaying()) {
                dialogVideo.stopPlayback();
            }
            dialog.dismiss();
        });

        if (mediaType.equals("image")) {
            tvMediaType.setText("Photo Evidence");
            dialogImage.setVisibility(View.VISIBLE);
            dialogVideo.setVisibility(View.GONE);

            // Use Glide for loading the image
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            Glide.with(this)
                    .load(mediaUri)
                    .apply(requestOptions)
                    .into(dialogImage);

            dialogImage.setZoomable(true);
        } else {
            tvMediaType.setText("Video Evidence");
            dialogImage.setVisibility(View.GONE);
            dialogVideo.setVisibility(View.VISIBLE);

            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(dialogVideo);
            dialogVideo.setMediaController(mediaController);
            dialogVideo.setVideoURI(mediaUri);
            dialogVideo.requestFocus();

            dialogVideo.setOnPreparedListener(mp -> {
                mp.start();
                mp.setLooping(true);
            });
        }

        dialogView.setOnClickListener(v -> {
            if (dialogVideo.isPlaying()) {
                dialogVideo.stopPlayback();
            }
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void removeMedia() {
        mediaCardView.setVisibility(View.GONE);
        photoBitmap = null;
        mediaUri = null;
        mediaType = null;
        currentPhotoPath = null;
        currentVideoPath = null;
    }

    private void confirmAndSubmitReport() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Submission")
                .setMessage("Are you sure you want to submit this incident report?")
                .setPositiveButton("Submit", (dialog, which) -> submitReport())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReport() {
        String description = etDescription.getText().toString().trim();
        String locationText = etLocation.getText().toString().trim();
        String incidentType = incidentTypeSpinner.getSelectedItem().toString();
        boolean isAnonymous = cbAnonymous.isChecked();

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (locationText.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        progressDialog.show();

        if (mediaType != null) {
            uploadMediaAndSubmitReport(description, locationText, incidentType, isAnonymous);
        } else {
            submitReportToFirestore(description, locationText, incidentType, isAnonymous, null);
        }
    }

    private void uploadMediaAndSubmitReport(String description, String locationText,
                                            String incidentType, boolean isAnonymous) {
        StorageReference mediaRef = storage.getReference().child("incident_media/" + System.currentTimeMillis());
        UploadTask uploadTask;

        if (mediaType.equals("image")) {
            // Check if we have a file path from camera
            if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                File imageFile = new File(currentPhotoPath);
                if (imageFile.exists()) {
                    uploadTask = mediaRef.putFile(Uri.fromFile(imageFile));
                } else {
                    // Fall back to the media URI
                    uploadTask = mediaRef.putFile(mediaUri);
                }
            } else {
                // If no file path, use the URI
                uploadTask = mediaRef.putFile(mediaUri);
            }
        } else {
            uploadTask = mediaRef.putFile(mediaUri);
        }

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog.setMessage("Uploading media... " + (int) progress + "%");
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                mediaRef.getDownloadUrl().addOnCompleteListener(uriTask -> {
                    if (uriTask.isSuccessful()) {
                        submitReportToFirestore(description, locationText, incidentType, isAnonymous, uriTask.getResult().toString());
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(ReportIncidentActivity.this, "Failed to get media URL", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressDialog.dismiss();
                Toast.makeText(ReportIncidentActivity.this, "Failed to upload media", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReportToFirestore(String description, String locationText,
                                         String incidentType, boolean isAnonymous, String mediaUrl) {
        Map<String, Object> incidentData = new HashMap<>();
        incidentData.put("type", incidentType);
        incidentData.put("description", description);
        incidentData.put("locationText", locationText);
        incidentData.put("timestamp", System.currentTimeMillis());
        incidentData.put("status", "pending");
        incidentData.put("anonymous", isAnonymous);
        incidentData.put("hasMedia", mediaUrl != null);

        if (mediaUrl != null) {
            incidentData.put("mediaUrl", mediaUrl);
            incidentData.put("mediaType", mediaType);
        }

        if (!isAnonymous && currentUser != null) {
            incidentData.put("userId", currentUser.getUid());
            incidentData.put("userName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
            incidentData.put("userEmail", currentUser.getEmail() != null ? currentUser.getEmail() : "");
            String phoneNumber = currentUser.getPhoneNumber();
            incidentData.put("userPhone", phoneNumber != null ? phoneNumber : "");
        }

        fStore.collection("incidents")
                .add(incidentData)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportIncidentActivity.this, "Incident reported successfully", Toast.LENGTH_SHORT).show();

                    // Send push notification to security staff
                    sendIncidentNotification(documentReference.getId(), incidentType, locationText);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(ReportIncidentActivity.this, "Failed to report incident: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendIncidentNotification(String incidentId, String incidentType, String location) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                // Create notification data with deep link
                JSONObject additionalData = new JSONObject()
                        .put("incident_id", incidentId)
                        .put("type", "new_incident")
                        .put("action", "view_incident")
                        .put("deep_link", "https://example.com/incidents/" + incidentId);

                // Target only security staff
                JSONArray filters = new JSONArray()
                        .put(new JSONObject().put("field", "tag").put("key", "user_role").put("relation", "=").put("value", "Security"));

                String notificationTitle = "New Incident Reported";
                String notificationMessage = String.format("%s at %s", incidentType, location);

                JSONObject payload = new JSONObject()
                        .put("app_id", ApplicationClass.ONESIGNAL_APP_ID)
                        .put("filters", filters)
                        .put("contents", new JSONObject().put("en", notificationMessage))
                        .put("headings", new JSONObject().put("en", notificationTitle))
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
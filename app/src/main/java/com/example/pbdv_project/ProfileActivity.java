package com.example.pbdv_project;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import android.Manifest;


public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY_PERMISSION = 101;

    private TextInputEditText editTextName, editTextEmail, editTextPhone, editTextRole;
    private TextInputEditText editTextCurrentPassword, editTextNewPassword, editTextConfirmPassword;
    private MaterialButton buttonSaveProfile;
    private ImageView profileImage;
    private FloatingActionButton fabEditPhoto;
    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    private Uri imageUri;
    private Uri cameraPhotoUri;
    private boolean removeImage = false;

    // Activity result launcher for picking images
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        startUCrop(sourceUri);
                    }
                }
            }
    );

    // Activity result launcher for taking photos
    private final ActivityResultLauncher<Intent> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (cameraPhotoUri != null) {
                        startUCrop(cameraPhotoUri);
                    } else {
                        Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Activity result launcher for UCrop
    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = UCrop.getOutput(result.getData());
                    if (imageUri != null) {
                        try {
                            Glide.with(this)
                                    .load(imageUri)
                                    .circleCrop()
                                    .into(profileImage);
                            removeImage = false;

                            // Don't clear cameraPhotoUri here - we still need it for upload
                            // cameraPhotoUri = null;
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading cropped image: " + e.getMessage());
                            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    Log.e(TAG, "Crop error: " + cropError);
                    Toast.makeText(this, "Image cropping failed", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void takeCameraPhoto() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Create the file where the photo should go
            File photoFile = createImageFile();

            if (photoFile != null) {
                cameraPhotoUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        photoFile
                );

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    takePhotoLauncher.launch(takePictureIntent);
                } else {
                    Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error taking photo: " + e.getMessage(), e);
            Toast.makeText(this, "Error preparing camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            takeCameraPhoto();
        }
    }

    private void checkGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API level 33) requires READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_GALLERY_PERMISSION);
            } else {
                launchGalleryIntent();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_GALLERY_PERMISSION);
            } else {
                launchGalleryIntent();
            }
        }
    }

    private void launchGalleryIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickPhotoIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takeCameraPhoto();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchGalleryIntent();
            } else {
                Toast.makeText(this, "Permission is required to select images",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create a temporary image file
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getCacheDir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    private void startUCrop(Uri sourceUri) {
        try {
            String destinationFileName = "cropped_" + UUID.randomUUID().toString() + ".jpg";
            File destinationFile = new File(getCacheDir(), destinationFileName);
            Uri destinationUri = Uri.fromFile(destinationFile);

            UCrop.Options options = new UCrop.Options();
            options.setCompressionQuality(90);
            options.setCircleDimmedLayer(true);
            options.setShowCropFrame(false);
            options.setHideBottomControls(false);
            options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
            options.setToolbarTitle("Crop Image");

            // Launch UCrop activity
            Intent intent = UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(500, 500)
                    .withOptions(options)
                    .getIntent(this);

            cropImageLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting UCrop: " + e.getMessage(), e);
            Toast.makeText(this, "Error preparing image cropper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth, Firestore, and Storage
        user = FirebaseAuth.getInstance().getCurrentUser();
        fStore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Update Your Profile");

            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon != null) {
                navIcon.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
            }

            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextRole = findViewById(R.id.editTextRole);
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);
        profileImage = findViewById(R.id.profileImage);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);

        loadUserData();
        loadProfileImage();

        buttonSaveProfile.setOnClickListener(v -> validateAndSaveChanges());
        fabEditPhoto.setOnClickListener(v -> showImagePickerDialog());
        profileImage.setOnClickListener(v -> viewFullSizeImage());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Photo", "Cancel"};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            try {
                if (which == 0) {
                    // Take photo with camera
                    checkCameraPermission();
                } else if (which == 1) {
                    // Choose from gallery
                    checkGalleryPermission();
                } else if (which == 2) {
                    // Remove photo
                    removeImage = true;
                    profileImage.setImageResource(R.drawable.ic_person_improved);
                    imageUri = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in image picker: " + e.getMessage());
                Toast.makeText(this, "Error selecting image", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void viewFullSizeImage() {
        if ((user.getPhotoUrl() != null && !removeImage) || imageUri != null) {
            Uri imageUriToShow = imageUri != null ? imageUri : user.getPhotoUrl();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullsize_image, null);

            ImageView fullSizeImageView = dialogView.findViewById(R.id.fullSizeImageView);
            MaterialButton closeButton = dialogView.findViewById(R.id.buttonCloseImage);

            try {
                Glide.with(this)
                        .load(imageUriToShow)
                        .placeholder(R.drawable.ic_person_improved)
                        .error(R.drawable.ic_person_improved)
                        .into(fullSizeImageView);

                AlertDialog dialog = builder.setView(dialogView)
                        .setCancelable(true)
                        .create();

                closeButton.setOnClickListener(v -> dialog.dismiss());

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing full size image: " + e.getMessage());
                Toast.makeText(this, "Error displaying image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No profile image to display", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage() {
        try {
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
                                    Glide.with(ProfileActivity.this)
                                            .load(photoUrl)
                                            .circleCrop()
                                            .placeholder(R.drawable.ic_person_improved)
                                            .into(profileImage);
                                } else {
                                    profileImage.setImageResource(R.drawable.ic_person_improved);
                                }
                            } else {
                                profileImage.setImageResource(R.drawable.ic_person_improved);
                            }
                        })
                        .addOnFailureListener(e -> {
                            profileImage.setImageResource(R.drawable.ic_person_improved);
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile image: " + e.getMessage());
            profileImage.setImageResource(R.drawable.ic_person_improved);
        }
    }

    private void loadUserData() {
        progressDialog = ProgressDialog.show(this, "", "Loading profile...", true);

        try {
            editTextEmail.setText(user.getEmail());

            if (user.getDisplayName() != null) {
                editTextName.setText(user.getDisplayName());
            }

            DocumentReference userRef = fStore.collection("users").document(user.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    if (TextUtils.isEmpty(editTextName.getText())) {
                        String name = documentSnapshot.getString("name");
                        if (name != null) {
                            editTextName.setText(name);
                        }
                    }

                    String phone = documentSnapshot.getString("phone");
                    if (phone != null) {
                        editTextPhone.setText(phone);
                    }

                    String role = documentSnapshot.getString("role");
                    if (role != null) {
                        editTextRole.setText(role);
                    }
                }
                dismissProgressDialog();
            }).addOnFailureListener(e -> {
                Toast.makeText(ProfileActivity.this, "Failed to load profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                dismissProgressDialog();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data: " + e.getMessage());
            Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            dismissProgressDialog();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog: " + e.getMessage());
            }
        }
    }

    private void validateAndSaveChanges() {
        String name = Objects.requireNonNull(editTextName.getText()).toString().trim();
        String phone = Objects.requireNonNull(editTextPhone.getText()).toString().trim();
        String currentPassword = Objects.requireNonNull(editTextCurrentPassword.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(editTextNewPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(editTextConfirmPassword.getText()).toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required");
            editTextPhone.requestFocus();
            return;
        }

        boolean passwordChangeRequested = !TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmPassword);

        if (passwordChangeRequested) {
            if (TextUtils.isEmpty(currentPassword)) {
                editTextCurrentPassword.setError("Current password is required");
                editTextCurrentPassword.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(newPassword)) {
                editTextNewPassword.setError("New password is required");
                editTextNewPassword.requestFocus();
                return;
            }

            if (newPassword.length() < 8) {
                editTextNewPassword.setError("Password must be at least 8 characters");
                editTextNewPassword.requestFocus();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                editTextConfirmPassword.setError("Passwords do not match");
                editTextConfirmPassword.requestFocus();
                return;
            }
        }

        progressDialog = ProgressDialog.show(this, "", "Updating profile...", true);

        try {
            if (removeImage) {
                updateProfileInfo(name, phone, "");
            } else if (imageUri != null) {
                uploadImageToFirebase();
            } else if (passwordChangeRequested) {
                reAuthenticateAndUpdatePassword(currentPassword, newPassword);
            } else {
                updateProfileInfo(name, phone, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during save process: " + e.getMessage());
            dismissProgressDialog();
            Toast.makeText(this, "Error saving profile changes", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase() {
        Uri uploadUri = imageUri != null ? imageUri : cameraPhotoUri;
        if (uploadUri == null) {
            dismissProgressDialog();
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String imageName = "user-" + user.getUid() + "-" + UUID.randomUUID().toString() + ".jpg";
            StorageReference imageRef = storageReference.child("profile_images/" + imageName);

            imageRef.putFile(uploadUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            String name = Objects.requireNonNull(editTextName.getText()).toString().trim();
                            String phone = Objects.requireNonNull(editTextPhone.getText()).toString().trim();
                            String currentPassword = Objects.requireNonNull(editTextCurrentPassword.getText()).toString().trim();
                            String newPassword = Objects.requireNonNull(editTextNewPassword.getText()).toString().trim();

                            boolean passwordChangeRequested = !TextUtils.isEmpty(newPassword);

                            if (passwordChangeRequested) {
                                reAuthenticateAndUpdatePassword(currentPassword, newPassword);
                            } else {
                                updateProfileInfo(name, phone, imageUrl);
                            }

                            // Now we can clear the camera photo URI after successful upload
                            cameraPhotoUri = null;
                        });
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        Toast.makeText(ProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image: " + e.getMessage());
            dismissProgressDialog();
            Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void reAuthenticateAndUpdatePassword(String currentPassword, String newPassword) {
        try {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    updatePassword(newPassword);
                } else {
                    dismissProgressDialog();
                    Toast.makeText(ProfileActivity.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error during reauthentication: " + e.getMessage());
            dismissProgressDialog();
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePassword(String newPassword) {
        try {
            user.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String name = Objects.requireNonNull(editTextName.getText()).toString().trim();
                    String phone = Objects.requireNonNull(editTextPhone.getText()).toString().trim();
                    updateProfileInfo(name, phone, null);
                } else {
                    dismissProgressDialog();
                    Toast.makeText(ProfileActivity.this, "Failed to update password: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating password: " + e.getMessage());
            dismissProgressDialog();
            Toast.makeText(this, "Error updating password", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileInfo(String name, String phone, String photoUrl) {
        try {
            UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name);

            if (photoUrl != null && !photoUrl.isEmpty()) {
                profileUpdatesBuilder.setPhotoUri(Uri.parse(photoUrl));
            } else if (photoUrl != null && photoUrl.isEmpty()) {
                profileUpdatesBuilder.setPhotoUri(null);
            }

            UserProfileChangeRequest profileUpdates = profileUpdatesBuilder.build();

            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentReference userRef = fStore.collection("users").document(user.getUid());
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", name);
                    updates.put("phone", phone);

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        updates.put("photoUrl", photoUrl);
                    } else if (photoUrl != null && photoUrl.isEmpty()) {
                        updates.put("photoUrl", "");
                    }

                    userRef.update(updates).addOnSuccessListener(aVoid -> {
                        dismissProgressDialog();
                        showSuccessDialog();

                        Intent resultIntent = new Intent();
                        if (photoUrl != null) {
                            resultIntent.putExtra("photoUrl", photoUrl);
                        }
                        setResult(RESULT_OK, resultIntent);
                    }).addOnFailureListener(e -> {
                        dismissProgressDialog();
                        Toast.makeText(ProfileActivity.this, "Failed to update profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    dismissProgressDialog();
                    Toast.makeText(ProfileActivity.this, "Failed to update profile: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating profile info: " + e.getMessage());
            dismissProgressDialog();
            Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Profile Updated")
                    .setMessage("Your profile has been successfully updated.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        editTextCurrentPassword.setText("");
                        editTextNewPassword.setText("");
                        editTextConfirmPassword.setText("");
                        setResult(RESULT_OK);
                        finish();
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing success dialog: " + e.getMessage());
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();

        // Clean up temporary files
        if (cameraPhotoUri != null) {
            File file = new File(cameraPhotoUri.getPath());
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
package com.example.pbdv_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewActivity extends AppCompatActivity {

    private FirebaseUser user;
    private FirebaseFirestore fStore;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_view);

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        fStore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        TextView textName = findViewById(R.id.textName);
        TextView textEmail = findViewById(R.id.textEmail);
        TextView textPhone = findViewById(R.id.textPhone);
        TextView textRole = findViewById(R.id.textRole);
        Button buttonEditProfile = findViewById(R.id.buttonEditProfile);

        // Set click listener for profile image
        profileImage.setOnClickListener(v -> viewFullSizeImage());

        // Load user data
        if (user != null) {
            textEmail.setText(user.getEmail());
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                textName.setText(user.getDisplayName());
            }

            // Load profile image immediately
            loadProfileImage();

            // Load additional info from Firestore
            DocumentReference userRef = fStore.collection("users").document(user.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    if (name != null && !name.isEmpty()) {
                        textName.setText(name);
                    }

                    String phone = documentSnapshot.getString("phone");
                    if (phone != null) {
                        textPhone.setText(phone);
                    }

                    String role = documentSnapshot.getString("role");
                    if (role != null) {
                        textRole.setText(role);
                    }

                    // Refresh profile image after getting additional data
                    loadProfileImage();
                }
            });
        }

        // Set up edit profile button
        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileViewActivity.this, ProfileActivity.class);
            startActivityForResult(intent, 1);
        });
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
                                    Glide.with(ProfileViewActivity.this)
                                            .load(photoUrl)
                                            .circleCrop()
                                            .placeholder(R.drawable.ic_person_improved)
                                            .into(profileImage);
                                } else {
                                    // Default image with proper dimensions
                                    profileImage.setImageResource(R.drawable.ic_person_improved);
                                    profileImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
            profileImage.setImageResource(R.drawable.ic_person_improved);
        }
    }

    private void viewFullSizeImage() {
        // Check if there's an image to display
        boolean hasImage = false;
        String imageUrl = null;

        if (user.getPhotoUrl() != null) {
            hasImage = true;
            imageUrl = user.getPhotoUrl().toString();
        } else {
            // Check Firestore for a custom photo URL
            fStore.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                showFullSizeImageDialog(photoUrl);
                            } else {
                                Toast.makeText(this, "No profile image to display", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No profile image to display", Toast.LENGTH_SHORT).show();
                        }
                    });
            return;
        }

        if (hasImage && imageUrl != null) {
            showFullSizeImageDialog(imageUrl);
        }
    }

    private void showFullSizeImageDialog(String imageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullsize_image, null);

        ImageView fullSizeImageView = dialogView.findViewById(R.id.fullSizeImageView);
        Button closeButton = dialogView.findViewById(R.id.buttonCloseImage);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_person_improved)
                .error(R.drawable.ic_person_improved)
                .into(fullSizeImageView);

        AlertDialog dialog = builder.setView(dialogView).create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {

            loadProfileImage();

            // Refresh user data from Firestore
            if (user != null) {
                DocumentReference userRef = fStore.collection("users").document(user.getUid());
                userRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        TextView textName = findViewById(R.id.textName);
                        TextView textPhone = findViewById(R.id.textPhone);
                        TextView textRole = findViewById(R.id.textRole);

                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            textName.setText(name);
                        }

                        String phone = documentSnapshot.getString("phone");
                        if (phone != null) {
                            textPhone.setText(phone);
                        }

                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            textRole.setText(role);
                        }
                    }
                });
            }
        }
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
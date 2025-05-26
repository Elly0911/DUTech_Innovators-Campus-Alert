package com.example.pbdv_project;

import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText editTextName, editTextEmail, editTextPassword, editTextPhone;
    Spinner roleSpinner;
    Button buttonReg;
    FirebaseAuth mAuth;
    FirebaseFirestore fStore;
    TextView textView;
    private static final String STUDENT_EMAIL_DOMAIN = "@dut4life.ac.za";

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            redirectBasedOnRole(currentUser.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        editTextName = findViewById(R.id.name);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextPhone = findViewById(R.id.phone);
        roleSpinner = findViewById(R.id.roleSpinner);
        buttonReg = findViewById(R.id.btn_register);
        textView = findViewById(R.id.loginNow);

        // Setup role spinner with hint
        String[] roles = getResources().getStringArray(R.array.user_roles);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setText("Choose Role");
                    textView.setTextColor(Color.GRAY);
                } else {
                    textView.setText(roles[position-1]); // Offset by 1 to account for hint
                    textView.setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setText("Choose Role");
                    textView.setTextColor(Color.GRAY);
                } else {
                    textView.setText(roles[position-1]); // Offset by 1 to account for hint
                    textView.setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public int getCount() {
                return roles.length + 1; // +1 for the hint
            }

            @Override
            public String getItem(int position) {
                return position == 0 ? "Choose Role" : roles[position-1];
            }

            @Override
            public boolean isEnabled(int position) {
                return position != 0; // Disable the hint item
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        roleSpinner.setSelection(0, false);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                String name, email, password, phone, role;
                name = String.valueOf(editTextName.getText());
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());
                phone = String.valueOf(editTextPhone.getText());
                role = roleSpinner.getSelectedItemPosition() == 0 ? "" : roleSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(name)){
                    Toast.makeText(RegisterActivity.this, "Enter name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(email)){
                    Toast.makeText(RegisterActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(phone)){
                    Toast.makeText(RegisterActivity.this, "Enter phone number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)){
                    Toast.makeText(RegisterActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 8) {
                    Toast.makeText(RegisterActivity.this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (role.isEmpty()){
                    Toast.makeText(RegisterActivity.this, "Please select a role", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if student email is valid
                if ("Student".equals(role) && !email.endsWith(STUDENT_EMAIL_DOMAIN)) {
                    Toast.makeText(RegisterActivity.this,
                            "Student email must end with " + STUDENT_EMAIL_DOMAIN,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name)
                                                .build();

                                        user.updateProfile(profileUpdates)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            String userId = user.getUid();
                                                            DocumentReference documentReference = fStore.collection("users").document(userId);

                                                            Map<String, Object> userData = new HashMap<>();
                                                            userData.put("name", name);
                                                            userData.put("email", email);
                                                            userData.put("phone", phone);
                                                            userData.put("role", role);

                                                            documentReference.set(userData)
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                Toast.makeText(RegisterActivity.this, "Account Created.", Toast.LENGTH_SHORT).show();
                                                                                redirectBasedOnRole(userId);
                                                                            } else {
                                                                                Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void redirectBasedOnRole(String userId) {
        fStore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Intent intent;

                        if (role != null) {
                            switch (role) {
                                case "Security":
                                    intent = new Intent(getApplicationContext(), SecurityDashboardActivity.class);
                                    break;
                                case "Staff":
                                    intent = new Intent(getApplicationContext(), StaffDashboardActivity.class);
                                    break;
                                case "Student":
                                default:
                                    intent = new Intent(getApplicationContext(), StudentDashboardActivity.class);
                                    break;
                            }
                            startActivity(intent);
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Intent intent = new Intent(getApplicationContext(), StudentDashboardActivity.class);
                    startActivity(intent);
                    finish();
                });
    }
}
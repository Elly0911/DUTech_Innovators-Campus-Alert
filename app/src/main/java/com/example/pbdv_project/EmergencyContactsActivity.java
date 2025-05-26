package com.example.pbdv_project;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class EmergencyContactsActivity extends AppCompatActivity {
    private FirebaseFirestore fStore;
    private FirebaseUser user;
    private CollectionReference contactsRef;
    private EmergencyContactAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_contacts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please sign in to manage emergency contacts", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        contactsRef = fStore.collection("users").document(user.getUid())
                .collection("emergency_contacts");

        // Initialize UI components
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAddContact = findViewById(R.id.fab_add_contact);
        fabAddContact.setOnClickListener(v -> showAddContactDialog());

        setUpRecyclerView();
        checkIfContactsEmpty();
    }

    private void checkIfContactsEmpty() {
        contactsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isEmpty = task.getResult().isEmpty();
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setUpRecyclerView() {
        Query query = contactsRef.orderBy("name", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<EmergencyContact> options = new FirestoreRecyclerOptions.Builder<EmergencyContact>()
                .setQuery(query, EmergencyContact.class)
                .build();

        adapter = new EmergencyContactAdapter(options);
        RecyclerView recyclerView = findViewById(R.id.contacts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((documentSnapshot, position) -> {
            EmergencyContact contact = documentSnapshot.toObject(EmergencyContact.class);
            if (contact != null) {
                showEditDeleteDialog(documentSnapshot.getId(), contact);
            }
        });

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkIfContactsEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkIfContactsEmpty();
            }
        });
    }

    private void showAddContactDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_contact, null);
        dialogBuilder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_phone);
        EditText editRelationship = dialogView.findViewById(R.id.edit_relationship);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String relationship = editRelationship.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show();
                return;
            }

            EmergencyContact contact = new EmergencyContact(name, phone, relationship);
            contactsRef.add(contact)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Emergency contact added successfully", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error adding contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showEditDeleteDialog(String contactId, EmergencyContact contact) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_contact, null);
        dialogBuilder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_phone);
        EditText editRelationship = dialogView.findViewById(R.id.edit_relationship);
        Button btnUpdate = dialogView.findViewById(R.id.btn_update);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);

        editName.setText(contact.getName());
        editPhone.setText(contact.getPhone());
        editRelationship.setText(contact.getRelationship());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        btnUpdate.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String relationship = editRelationship.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show();
                return;
            }

            EmergencyContact updatedContact = new EmergencyContact(name, phone, relationship);
            contactsRef.document(contactId).set(updatedContact)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Emergency contact updated successfully", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Emergency Contact")
                    .setMessage("Are you sure you want to delete this emergency contact?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        contactsRef.document(contactId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Emergency contact deleted successfully", Toast.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error deleting contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
            checkIfContactsEmpty();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
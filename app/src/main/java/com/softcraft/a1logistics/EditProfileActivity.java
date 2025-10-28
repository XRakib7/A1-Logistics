package com.softcraft.a1logistics;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditProfileActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private String userRole;
    private String userId;

    // Views
    private ImageView profileImage;
    private TextInputEditText fullNameEditText, emailEditText, phoneEditText, businessNameEditText, addressEditText;
    private Button changePictureButton, cancelButton, saveButton;
    private LinearProgressIndicator progressIndicator;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Get current user
        Map<String, String> user = getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        userRole = user.get("role");
        userId = user.get("uid");

        initializeViews();
        setupClickListeners();
        loadUserProfile();

        // Show/hide merchant-specific fields
        setupRoleSpecificViews();
    }

    private void initializeViews() {
        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Profile image
        profileImage = findViewById(R.id.profileImage);
        changePictureButton = findViewById(R.id.changePictureButton);

        // Form fields
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        businessNameEditText = findViewById(R.id.businessNameEditText);
        addressEditText = findViewById(R.id.addressEditText);

        // Buttons
        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveButton);

        // Progress indicator
        progressIndicator = findViewById(R.id.progressIndicator);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // No menu for settings
        return false;
    }

    private void setupRoleSpecificViews() {
        if ("Merchant".equals(userRole)) {
            businessNameEditText.setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.businessNameLayout).setVisibility(android.view.View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        changePictureButton.setOnClickListener(v -> openImageChooser());
        cancelButton.setOnClickListener(v -> onBackPressed());
        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserProfile() {
        String collectionName = "Admin".equals(userRole) ? "Admins" : "Merchants";

        db.collection(collectionName).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        populateFormFields(document);
                    }
                });
    }

    private void populateFormFields(DocumentSnapshot document) {
        // Basic user info
        String name = document.getString("Admin".equals(userRole) ? "adminName" : "businessName");
        String email = document.getString("email");
        String phone = document.getString("phone");
        String businessName = document.getString("businessName");
        String address = document.getString("address");

        fullNameEditText.setText(name != null ? name : "");
        emailEditText.setText(email != null ? email : "");
        phoneEditText.setText(phone != null ? phone : "");
        addressEditText.setText(address != null ? address : "");

        if (businessName != null) {
            businessNameEditText.setText(businessName);
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            // Here you would typically upload the image to Firebase Storage
            Toast.makeText(this, "Profile picture selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileChanges() {
        String fullName = Objects.requireNonNull(fullNameEditText.getText()).toString().trim();
        String phone = Objects.requireNonNull(phoneEditText.getText()).toString().trim();
        String address = Objects.requireNonNull(addressEditText.getText()).toString().trim();
        String businessName = businessNameEditText.getText() != null ?
                businessNameEditText.getText().toString().trim() : "";

        // Validate inputs
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Full name is required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("Phone number is required");
            return;
        }

        if ("Merchant".equals(userRole) && TextUtils.isEmpty(businessName)) {
            businessNameEditText.setError("Business name is required");
            return;
        }

        showProgress(true);

        // Prepare update data
        Map<String, Object> updates = new HashMap<>();
        if ("Admin".equals(userRole)) {
            updates.put("adminName", fullName);
        } else {
            updates.put("businessName", fullName);
        }
        updates.put("phone", phone);
        updates.put("address", address);

        if ("Merchant".equals(userRole)) {
            updates.put("businessName", businessName);
        }

        String collectionName = "Admin".equals(userRole) ? "Admins" : "Merchants";

        db.collection(collectionName).document(userId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        // Update local preferences
                        updateLocalUserData(fullName);

                        // If image was selected, upload it
                        if (imageUri != null) {
                            uploadProfileImage();
                        } else {
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateLocalUserData(String name) {
        // Update local shared preferences or user data
        // This would depend on your BaseActivity implementation
    }

    private void uploadProfileImage() {
        if (imageUri != null) {
            StorageReference profileImageRef = storage.getReference()
                    .child("profile_images")
                    .child(userId + ".jpg");

            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            finish();
        }
    }

    private void showProgress(boolean show) {
        progressIndicator.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        saveButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
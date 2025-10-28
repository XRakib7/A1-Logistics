package com.softcraft.a1logistics;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;
import java.util.regex.Pattern;

public class ChangePasswordActivity extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // Views
    private TextInputEditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button cancelButton, changePasswordButton;
    private LinearProgressIndicator progressIndicator;

    // Password validation pattern
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            logout();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Change Password");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Form fields
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        // Buttons
        cancelButton = findViewById(R.id.cancelButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);

        // Progress indicator
        progressIndicator = findViewById(R.id.progressIndicator);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // No menu for settings
        return false;
    }
    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> onBackPressed());
        changePasswordButton.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String currentPassword = Objects.requireNonNull(currentPasswordEditText.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(newPasswordEditText.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordEditText.getText()).toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordEditText.setError("Current password is required");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            newPasswordEditText.setError("New password is required");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Please confirm your new password");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        if (!isValidPassword(newPassword)) {
            newPasswordEditText.setError("Password does not meet requirements");
            return;
        }

        showProgress(true);

        // Re-authenticate user first
        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), currentPassword
        );

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User re-authenticated, now change password
                        updatePassword(newPassword);
                    } else {
                        showProgress(false);
                        currentPasswordEditText.setError("Current password is incorrect");
                        Toast.makeText(ChangePasswordActivity.this,
                                "Authentication failed. Please check your current password.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePassword(String newPassword) {
        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(ChangePasswordActivity.this,
                                "Password updated successfully",
                                Toast.LENGTH_SHORT).show();

                        // Clear form
                        clearForm();

                        // Show success message and finish
                        showSuccessDialog();
                    } else {
                        String errorMessage = "Failed to update password. Please try again.";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(ChangePasswordActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private void clearForm() {
        currentPasswordEditText.setText("");
        newPasswordEditText.setText("");
        confirmPasswordEditText.setText("");
    }

    private void showSuccessDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Password Changed")
                .setMessage("Your password has been changed successfully. You will need to use your new password for your next login.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showProgress(boolean show) {
        progressIndicator.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        changePasswordButton.setEnabled(!show);
        cancelButton.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
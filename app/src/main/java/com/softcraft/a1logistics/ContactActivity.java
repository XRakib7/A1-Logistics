package com.softcraft.a1logistics;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public class ContactActivity extends AppCompatActivity {

    private LottieAnimationView contactAnimation;
    private TextInputEditText nameInput, emailInput, phoneInput, messageInput;
    private MaterialButton submitButton;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupAnimations();
        setupSubmitButton();
        setupToolbarNavigation();
    }

    private void initializeViews() {
        contactAnimation = findViewById(R.id.contactAnimation);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        messageInput = findViewById(R.id.messageInput);
        submitButton = findViewById(R.id.submitButton);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void setupAnimations() {
        contactAnimation.setSpeed(0.8f);
        contactAnimation.playAnimation();
    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> {
            if (validateForm()) {
                progressDialog.setMessage("Sending your message...");
                progressDialog.show();
                submitButton.setEnabled(false);

                // Create message data
                Map<String, Object> message = new HashMap<>();
                message.put("name", nameInput.getText().toString().trim());
                message.put("email", emailInput.getText().toString().trim());
                message.put("phone", phoneInput.getText().toString().trim());
                message.put("message", messageInput.getText().toString().trim());
                message.put("timestamp", System.currentTimeMillis());
                message.put("status", "unread"); // Track message status

                // Add to "contact_messages" collection
                db.collection("contact_messages")
                        .add(message)
                        .addOnSuccessListener(documentReference -> {
                            // Send notification to admins
                            sendAdminNotification(
                                    nameInput.getText().toString().trim(),
                                    messageInput.getText().toString().trim()
                            );

                            Toast.makeText(ContactActivity.this,
                                    "Message sent successfully!", Toast.LENGTH_SHORT).show();
                            clearForm();
                            progressDialog.dismiss();
                            submitButton.setEnabled(true);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ContactActivity.this,
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            submitButton.setEnabled(true);
                        });
            }
        });
    }

    private void sendAdminNotification(String senderName, String messageText) {
        // Create notification data
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "New message from " + senderName);
        notification.put("body", messageText.length() > 50 ?
                messageText.substring(0, 50) + "..." : messageText);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("type", "new_message");

        // Send to admin notifications collection
        db.collection("admin_notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Also send FCM push notification
                    sendFCMPushToAdmins(senderName, messageText);
                })
                .addOnFailureListener(e -> {
                    // Log error but don't show to user
                    System.out.println("Failed to create admin notification: " + e.getMessage());
                });
    }

    private void sendFCMPushToAdmins(String senderName, String messageText) {
        // This requires a Cloud Function to handle the actual push notification
        // Here we just create a trigger document

        Map<String, Object> pushRequest = new HashMap<>();
        pushRequest.put("sender", senderName);
        pushRequest.put("message", messageText);
        pushRequest.put("timestamp", System.currentTimeMillis());
        pushRequest.put("processed", false);

        db.collection("push_requests")
                .add(pushRequest)
                .addOnFailureListener(e -> {
                    System.out.println("Failed to create push request: " + e.getMessage());
                });
    }

    private void setupToolbarNavigation() {
        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.helpButton).setOnClickListener(v -> showHelpDialog());
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (nameInput.getText().toString().trim().isEmpty()) {
            nameInput.setError("Please enter your name");
            isValid = false;
        }

        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            emailInput.setError("Please enter your email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            isValid = false;
        }

        if (messageInput.getText().toString().trim().isEmpty()) {
            messageInput.setError("Please enter your message");
            isValid = false;
        }

        return isValid;
    }

    private void clearForm() {
        nameInput.setText("");
        emailInput.setText("");
        phoneInput.setText("");
        messageInput.setText("");
        nameInput.clearFocus();
        emailInput.clearFocus();
        phoneInput.clearFocus();
        messageInput.clearFocus();
    }

    private void showHelpDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Contact Help")
                .setMessage("Fill out the form to send us a message. We typically respond within 24 hours.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        contactAnimation.resumeAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        contactAnimation.pauseAnimation();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
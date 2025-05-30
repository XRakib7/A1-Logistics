package com.softcraft.a1logistics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    private static final String COUNTERS_COLLECTION = "Counters";
    private static final String MERCHANT_COUNTER_DOC = "MerchantCounter";
    private static final String ADMIN_COUNTER_DOC = "AdminCounter";
    private static final String CONFIG_COLLECTION = "Config";
    private static final String ACCESS_CODES_DOC = "AccessCodes";
    private static final String ADMIN_CODE_FIELD = "adminCode";
    private static final int PHONE_VERIFICATION_REQUEST = 1001;
    private boolean isPhoneVerified = false;
    private String verifiedPhoneNumber = "";
    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI Components
    private TextInputEditText businessNameEditText, phoneEditText,
            pickupLocationEditText, emailEditText, passwordEditText;
    private TextInputLayout pickupLocationLayout, nameInputLayout;
    private MaterialButton verifyPhoneButton;
    private Button signupButton;
    private TextView signupTitle, loginRedirectText;

    // State variables
    private boolean isAdminSignup = false;
    private String currentName, currentPhone, currentPickupLocation, currentEmail;

    // UI Blocker
    private UIBlocker uiBlocker;
    private String adminCode = ""; // Will be loaded from Firestore
    private boolean adminCodeLoaded = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize UI Blocker
        uiBlocker = new UIBlocker(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
// Load admin code from Firestore
        loadAdminCode();
        // Initialize views
        initializeViews();
        setupListeners();
    }
    private void loadAdminCode() {
        uiBlocker.blockUI("Loading configuration...");

        db.collection(CONFIG_COLLECTION)
                .document(ACCESS_CODES_DOC)
                .get()
                .addOnCompleteListener(task -> {
                    uiBlocker.unblockUI();

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            adminCode = document.getString(ADMIN_CODE_FIELD);
                            adminCodeLoaded = true;
                            Log.d(TAG, "Admin code loaded successfully");
                        } else {
                            uiBlocker.showError("Configuration missing");
                            Log.w(TAG, "No such document");
                        }
                    } else {
                        uiBlocker.showError("Failed to load configuration");
                        Log.w(TAG, "Get failed with ", task.getException());
                    }
                });
    }

    private void initializeViews() {
        businessNameEditText = findViewById(R.id.businessNameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        verifyPhoneButton = findViewById(R.id.verifyPhoneButton);
        pickupLocationEditText = findViewById(R.id.pickupLocationEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.signupButton);
        pickupLocationLayout = findViewById(R.id.pickupLocationLayout);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        signupTitle = findViewById(R.id.signupTitle);
        loginRedirectText = findViewById(R.id.loginRedirectText);
    }

    private void setupListeners() {
        // Check for admin code when business name loses focus
        businessNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkForAdminCode();
            }
        });

        // Signup button click handler
        signupButton.setOnClickListener(v -> attemptSignup());

        // Login redirect
        loginRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
        verifyPhoneButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString().trim();
            if (phone.isEmpty()) {
                phoneEditText.setError("Enter phone number first");
                return;
            }
            if (!Patterns.PHONE.matcher(phone).matches()) {
                phoneEditText.setError("Enter valid phone number");
                return;
            }

            Intent intent = new Intent(SignupActivity.this, PhoneVerificationActivity.class);
            intent.putExtra("phoneNumber", phone);
            startActivityForResult(intent, PHONE_VERIFICATION_REQUEST);
        });
    }

    private void checkForAdminCode() {
        if (!adminCodeLoaded) {
            uiBlocker.showError("System not ready yet");
            return;
        }

        String businessName = businessNameEditText.getText().toString().trim();
        if (businessName.contains(adminCode)) {
            isAdminSignup = true;
            nameInputLayout.setHint("Admin Name");
            pickupLocationLayout.setVisibility(View.GONE);
            signupTitle.setText("Admin Sign Up");
            Snackbar.make(findViewById(android.R.id.content),
                    "Admin signup detected", Snackbar.LENGTH_LONG).show();

            // Auto-remove the code from visible text
            businessNameEditText.setText(businessName.replace(adminCode, "").trim());
        } else if (isAdminSignup) {
            // Revert to merchant UI if admin code was removed
            isAdminSignup = false;
            nameInputLayout.setHint("Business Name");
            pickupLocationLayout.setVisibility(View.VISIBLE);
            signupTitle.setText("Merchant Sign Up");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHONE_VERIFICATION_REQUEST) {
            if (resultCode == RESULT_OK) {
                isPhoneVerified = true;
                verifiedPhoneNumber = data.getStringExtra("phoneNumber");
                phoneEditText.setText(verifiedPhoneNumber);
                phoneEditText.setEnabled(false);
                verifyPhoneButton.setText("Verified");
                verifyPhoneButton.setEnabled(false);
                verifyPhoneButton.setIconResource(R.drawable.ic_verified);
                verifyPhoneButton.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.green)));
                Snackbar.make(findViewById(android.R.id.content),
                        "Phone number verified", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
    private void attemptSignup() {
        // Get input values
        String name = businessNameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String pickupLocation = isAdminSignup ? "" : pickupLocationEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Store current values - use the adminCode variable loaded from Firestore
        currentName = isAdminSignup ? name.replace(adminCode, "").trim() : name;
        currentPhone = phone;
        currentPickupLocation = pickupLocation;
        currentEmail = email;

        if (validateInputs(name, phone, pickupLocation, email, password)) {
            if (!adminCodeLoaded && isAdminSignup) {
                uiBlocker.showError("System configuration not loaded yet");
                signupButton.setEnabled(true);
                return;
            }

            uiBlocker.blockUI("Creating account...");
            signupButton.setEnabled(false);

            // Create Firebase auth user
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (isAdminSignup) {
                                    createAdminUser(currentName, currentPhone, currentEmail);
                                } else {
                                    createMerchantUser(currentName, currentPhone, currentPickupLocation, currentEmail);
                                }
                            }
                        } else {
                            uiBlocker.showError(task.getException().getMessage());
                            signupButton.setEnabled(true);
                        }
                    });
        } else {
            signupButton.setEnabled(true);
        }
    }
    private boolean validateInputs(String name, String phone, String pickupLocation,
                                   String email, String password) {
        boolean valid = true;

        if (name.isEmpty()) {
            businessNameEditText.setError(isAdminSignup ? "Admin name is required" : "Business name is required");
            valid = false;
        }

        if (phone.isEmpty()) {
            phoneEditText.setError("Phone number is required");
            valid = false;
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            phoneEditText.setError("Enter a valid phone number");
            valid = false;
        }/* else if (!isPhoneVerified || !phone.equals(verifiedPhoneNumber)) {
            phoneEditText.setError("Phone number must be verified");
            valid = false;
        }*/

        if (!isAdminSignup && pickupLocation.isEmpty()) {
            pickupLocationEditText.setError("Pickup location is required");
            valid = false;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            valid = false;
        }

        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            valid = false;
        }

        return valid;
    }

    private void createMerchantUser(String businessName, String phone,
                                    String pickupLocation, String email) {
        getNextUid(MERCHANT_COUNTER_DOC, "M", new UidCallback() {
            @Override
            public void onSuccess(String uid) {
                Map<String, Object> merchant = new HashMap<>();
                merchant.put("uid", uid);
                merchant.put("businessName", businessName);
                merchant.put("phone", phone);
                merchant.put("pickupLocation", pickupLocation);
                merchant.put("email", email);
                merchant.put("role", "Merchant");
                merchant.put("status", "Active");
                merchant.put("signupDate", FieldValue.serverTimestamp());

                db.collection("Merchants").document(uid)
                        .set(merchant)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                sendVerificationEmail();
                                uiBlocker.showSignupSuccess(() ->
                                        redirectToDashboard("Merchant"));
                            } else {
                                // Delete auth user if Firestore fails
                                mAuth.getCurrentUser().delete();
                                uiBlocker.showError("Failed to save merchant data");
                                signupButton.setEnabled(true);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                uiBlocker.showError("Failed to generate UID");
                signupButton.setEnabled(true);
            }
        });
    }

    private void createAdminUser(String adminName, String phone, String email) {
        getNextUid(ADMIN_COUNTER_DOC, "A", new UidCallback() {
            @Override
            public void onSuccess(String uid) {
                Map<String, Object> admin = new HashMap<>();
                admin.put("uid", uid);
                admin.put("adminName", adminName);
                admin.put("phone", phone);
                admin.put("email", email);
                admin.put("role", "Admin");
                admin.put("status", "Active");
                admin.put("signupDate", FieldValue.serverTimestamp());

                db.collection("Admins").document(uid)
                        .set(admin)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                sendVerificationEmail();
                                uiBlocker.showSignupSuccess(() ->
                                        redirectToDashboard("Admin"));
                            } else {
                                // Delete auth user if Firestore fails
                                mAuth.getCurrentUser().delete();
                                uiBlocker.showError("Failed to save admin data");
                                signupButton.setEnabled(true);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                uiBlocker.showError("Failed to generate UID");
                signupButton.setEnabled(true);
            }
        });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Email verification not sent", task.getException());
                        }
                    });
        }
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if (role.equals("Admin")) {
            intent = new Intent(SignupActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(SignupActivity.this, MerchantDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private interface UidCallback {
        void onSuccess(String uid);
        void onFailure(Exception e);
    }

    private void getNextUid(String counterDoc, String prefix, UidCallback callback) {
        DocumentReference counterRef = db.collection(COUNTERS_COLLECTION).document(counterDoc);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(counterRef);
            long nextId = 1;

            if (snapshot.exists()) {
                nextId = snapshot.getLong("lastId") + 1;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastId", nextId);
            transaction.set(counterRef, updates, SetOptions.merge());

            return prefix + String.format(Locale.getDefault(), "%03d", nextId);
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(task.getResult());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Clean up to prevent memory leaks
        if (uiBlocker != null) {
            uiBlocker.unblockUI();
        }
        super.onDestroy();
    }
}
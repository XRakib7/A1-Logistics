package com.softcraft.a1logistics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.File;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "A1LogisticsPrefs";
    private static final String USER_KEY = "currentUser";

    private MaterialCardView autoLoginCard;
    private LottieAnimationView autoLoginAnimation;
    private View overlayView;
    private TextView welcomeMessage;
    private ImageView closeButton;
    private MaterialButton continueButton;
    private static final String UPDATES_FOLDER = "updates";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File apkFile = new File(getExternalFilesDir(null), "update.apk");
        if (apkFile.exists()) {
            apkFile.delete();
        }
        initializeAutoLoginUI();
        checkExistingLogin();
        initializeMainUIComponents();
    }

    private void initializeAutoLoginUI() {
        autoLoginCard = findViewById(R.id.autoLoginCard);
        autoLoginAnimation = findViewById(R.id.autoLoginAnimation);
        overlayView = findViewById(R.id.overlayView);
        welcomeMessage = findViewById(R.id.welcomeMessage);
        closeButton = findViewById(R.id.closeButton);
        continueButton = findViewById(R.id.continueButton);
    }

    private void checkExistingLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userJson = prefs.getString(USER_KEY, null);

        if (userJson != null) {
            Map<String, String> userData = new Gson().fromJson(userJson, Map.class);
            showAutoLoginPrompt(userData);
        }
    }


    private void showAutoLoginPrompt(Map<String, String> userData) {
        String userName = userData.get("name");
        String userRole = userData.get("role");

        welcomeMessage.setText(String.format("Hello, %s!", userName));
        // Show UI elements with animation
        autoLoginCard.setVisibility(View.VISIBLE);
        overlayView.setVisibility(View.VISIBLE);

        autoLoginCard.setAlpha(0f);
        autoLoginCard.setScaleX(0.9f);
        autoLoginCard.setScaleY(0.9f);

        autoLoginCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        overlayView.setAlpha(0f);
        overlayView.animate()
                .alpha(0.7f)
                .setDuration(400)
                .start();

        // Start animation
        autoLoginAnimation.setAnimation(R.raw.wave_animation);
        autoLoginAnimation.playAnimation();

        // Set click listeners
        autoLoginCard.setOnClickListener(null);
        continueButton.setOnClickListener(v -> proceedToDashboard(userRole));
        closeButton.setOnClickListener(v -> dismissAutoLogin());
    }

    private void dismissAutoLogin() {
        autoLoginCard.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(300)
                .withEndAction(() -> autoLoginCard.setVisibility(View.GONE))
                .start();

        overlayView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> overlayView.setVisibility(View.GONE))
                .start();
    }

    private void proceedToDashboard(String role) {
        autoLoginAnimation.setAnimation(R.raw.loading);
        autoLoginAnimation.playAnimation();

        autoLoginCard.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(200)
                .withEndAction(() -> redirectToDashboard(role))
                .start();
    }

    private void redirectToDashboard(String role) {
        Intent intent = "Admin".equals(role) ?
                new Intent(this, AdminDashboardActivity.class) :
                new Intent(this, MerchantDashboardActivity.class);

        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void initializeMainUIComponents() {
        MaterialCardView loginCard = findViewById(R.id.loginCard);
        MaterialCardView signupCard = findViewById(R.id.signupCard);
        MaterialCardView servicesCard = findViewById(R.id.servicesCard);
        MaterialButton trackButton = findViewById(R.id.trackButton);
        TextInputEditText trackingNumberInput = findViewById(R.id.trackingNumberInput);

        // Set click listeners for all cards
        loginCard.setOnClickListener(v -> navigateToLogin());
        signupCard.setOnClickListener(v -> navigateToSignup());
        servicesCard.setOnClickListener(v -> navigateToServices());

        trackButton.setOnClickListener(v -> {
            String trackingNumber = trackingNumberInput.getText().toString().trim();
            trackShipment(trackingNumber.isEmpty() ? "" : trackingNumber);
        });

        // Bottom navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            if (itemId == R.id.nav_track) trackShipment("");
            if (itemId == R.id.nav_services) navigateToServices();
            if (itemId == R.id.nav_account) navigateToLogin();
            return false;
        });

        animateQuickActionCards();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void navigateToSignup() {
        startActivity(new Intent(this, SignupActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void trackShipment(String trackingNumber) {
        if (trackingNumber.isEmpty()) {
            // If no tracking number entered, just open TrackingActivity
            startActivity(new Intent(this, TrackingActivity.class));
        } else {
            // Validate tracking number format first
            if (isValidTrackingNumber(trackingNumber)) {
                Intent intent = new Intent(this, TrackingActivity.class);
                intent.putExtra("tracking_number", trackingNumber.trim());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid tracking number format", Toast.LENGTH_SHORT).show();
            }
        }
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }

    // Add this validation method
    private boolean isValidTrackingNumber(String trackingNumber) {
        // Basic validation for A1-M001-0001 format
        return trackingNumber.matches("^A1-[A-Za-z0-9]{3,4}-\\d{4}$");
    }
    private void navigateToServices() {
        startActivity(new Intent(this, ServicesActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void animateQuickActionCards() {
        MaterialCardView loginCard = findViewById(R.id.loginCard);
        MaterialCardView signupCard = findViewById(R.id.signupCard);
        MaterialCardView servicesCard = findViewById(R.id.servicesCard);

        loginCard.setAlpha(0f);
        loginCard.setScaleX(0.8f);
        loginCard.setScaleY(0.8f);

        signupCard.setAlpha(0f);
        signupCard.setScaleX(0.8f);
        signupCard.setScaleY(0.8f);

        servicesCard.setAlpha(0f);
        servicesCard.setScaleX(0.8f);
        servicesCard.setScaleY(0.8f);

        loginCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(100)
                .setDuration(300)
                .start();

        signupCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(200)
                .setDuration(300)
                .start();

        servicesCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(300)
                .setDuration(300)
                .start();
    }
}
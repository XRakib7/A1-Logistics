package com.softcraft.a1logistics;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationBarView;

public class ServicesActivity extends AppCompatActivity {

    private LottieAnimationView servicesHeroAnimation;
    private MaterialButton contactButton;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        initializeViews();
        setupAnimations();
        setupBottomNavigation();
        setupContactButton();
    }

    private void initializeViews() {
        servicesHeroAnimation = findViewById(R.id.servicesHeroAnimation);
        contactButton = findViewById(R.id.contactButton);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupAnimations() {
        servicesHeroAnimation.setSpeed(0.8f);
        servicesHeroAnimation.playAnimation();
    }

    private void setupBottomNavigation() {
        // Highlight services item
        bottomNavigationView.setSelectedItemId(R.id.nav_services);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_track) {
                startActivity(new Intent(this, TrackingActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_services) {
                // Already on services
                return true;
            } else if (itemId == R.id.nav_account) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupContactButton() {
        contactButton.setOnClickListener(v -> {
            // Open contact activity with slide animation
            startActivity(new Intent(this, ContactActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (servicesHeroAnimation != null) {
            servicesHeroAnimation.resumeAnimation();
        }
        // Ensure services item is selected when returning to this activity
        bottomNavigationView.setSelectedItemId(R.id.nav_services);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (servicesHeroAnimation != null) {
            servicesHeroAnimation.pauseAnimation();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Animate back to home when back button is pressed
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
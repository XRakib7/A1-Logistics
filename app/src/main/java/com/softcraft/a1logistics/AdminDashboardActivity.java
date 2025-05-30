package com.softcraft.a1logistics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

public class AdminDashboardActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Map<String, String> user = getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setText("Welcome Admin " + user.get("name"));

        // Initialize buttons
        Button activePackagesBtn = findViewById(R.id.activePackagesBtn);
        Button deliveredPackagesBtn = findViewById(R.id.deliveredPackagesBtn);
        Button returnedPackagesBtn = findViewById(R.id.returnedPackagesBtn);
        Button allMerchantsBtn = findViewById(R.id.allMerchantsBtn);
        Button allPackagesBtn = findViewById(R.id.allPackagesBtn);

        // Set click listeners
        // Set up buttons with package type extras
        findViewById(R.id.activePackagesBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, AllPackagesActivity.class)
                    .putExtra("packageType", "active"));
        });

        findViewById(R.id.deliveredPackagesBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, AllPackagesActivity.class)
                    .putExtra("packageType", "delivered"));
        });

        findViewById(R.id.returnedPackagesBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, AllPackagesActivity.class)
                    .putExtra("packageType", "returned"));
        });

        findViewById(R.id.allPackagesBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, AllPackagesActivity.class)
                    .putExtra("packageType", "all"));
        });

        findViewById(R.id.allMerchantsBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, AllMerchantsActivity.class));
        });


        // Add logout button functionality
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
    }
}
package com.softcraft.a1logistics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.Map;

public class MerchantDashboardActivity extends BaseActivity {

    private TextView activeCountText, deliveredCountText, returnedCountText, allCountText;
    private FirebaseFirestore db;
    private String merchantId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_dashboard);

        Map<String, String> user = getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        merchantId = user.get("uid");
        db = FirebaseFirestore.getInstance();

        // Initialize views
        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setText("Welcome, " + user.get("name"));

        // Initialize count TextViews
        activeCountText = findViewById(R.id.activeCountText);
        deliveredCountText = findViewById(R.id.deliveredCountText);
        returnedCountText = findViewById(R.id.returnedCountText);
        allCountText = findViewById(R.id.allCountText);

        // Initialize buttons
        MaterialButton createPickupButton = findViewById(R.id.createPickupButton);
        MaterialButton activePackagesButton = findViewById(R.id.activePackagesButton);
        MaterialButton deliveredPackagesButton = findViewById(R.id.deliveredPackagesButton);
        MaterialButton returnedPackagesButton = findViewById(R.id.returnedPackagesButton);
        MaterialButton allPackagesButton = findViewById(R.id.allPackagesButton);
        MaterialButton logoutButton = findViewById(R.id.logoutButton);

        // Set click listeners
        createPickupButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreatePickupActivity.class)));

        activePackagesButton.setOnClickListener(v ->
                startActivityWithPackageType("active"));

        deliveredPackagesButton.setOnClickListener(v ->
                startActivityWithPackageType("delivered"));

        returnedPackagesButton.setOnClickListener(v ->
                startActivityWithPackageType("returned"));

        allPackagesButton.setOnClickListener(v ->
                startActivityWithPackageType("all"));

        logoutButton.setOnClickListener(v -> logout());

        // Load package counts
        loadPackageCounts();
    }

    private void startActivityWithPackageType(String packageType) {
        Intent intent = new Intent(this, AllPackagesActivity.class);
        intent.putExtra("packageType", packageType);
        startActivity(intent);
    }

    private void loadPackageCounts() {
        // Active packages
        Query activeQuery = db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .whereIn("status", Arrays.asList(
                        "Pickup Pending",
                        "Picked Up",
                        "In Transit",
                        "Out For Delivery"
                ));

        // Delivered packages
        Query deliveredQuery = db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .whereIn("status", Arrays.asList("Delivered", "Paid To The Merchant"));

        // Returned packages
        Query returnedQuery = db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .whereIn("status", Arrays.asList(
                        "Returned By The Customer",
                        "Returned To The Merchant"
                ));

        // All packages
        Query allQuery = db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId);

        // Execute queries
        activeQuery.get().addOnCompleteListener(task ->
                activeCountText.setText(task.isSuccessful() ? String.valueOf(task.getResult().size()) : "0"));

        deliveredQuery.get().addOnCompleteListener(task ->
                deliveredCountText.setText(task.isSuccessful() ? String.valueOf(task.getResult().size()) : "0"));

        returnedQuery.get().addOnCompleteListener(task ->
                returnedCountText.setText(task.isSuccessful() ? String.valueOf(task.getResult().size()) : "0"));

        allQuery.get().addOnCompleteListener(task ->
                allCountText.setText(task.isSuccessful() ? String.valueOf(task.getResult().size()) : "0"));
    }
}
package com.softcraft.a1logistics;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class MerchantDashboardActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String merchantId;
    private String merchantName;

    // Views
    private TextView totalRevenueText, totalPackagesText, successRateText;
    private TextView activePackagesCount, deliveredPackagesCount, returnedPackagesCount, allCountText;
    private TextView revenueGrowthText, packagesGrowthText, returnRateText;
    private ProgressBar activeProgressBar;
    private PieChart packageDistributionChart;

    // Navigation
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Navigation Header Views
    private TextView navUsername, navEmail;
    private ImageView navProfileImage;

    // Quick Action Cards
    private CardView activePackagesCard, deliveredPackagesCard, returnedPackagesCard, allPackagesCard;

    // Statistics
    private int totalPackages = 0;
    private int activePackages = 0;
    private int deliveredPackages = 0;
    private int returnedPackages = 0;
    private double totalRevenue = 0.0;
    private double monthlyRevenue = 0.0;
    private double lastMonthRevenue = 0.0;

    // SharedPreferences
    private static final String PREFS_NAME = "A1LogisticsPrefs";
    private static final String USER_KEY = "currentUser";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Map<String, String> user = getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        merchantId = user.get("uid");
        merchantName = user.get("name");

        initializeViews();
        setupNavigation();
        loadMerchantData();
        loadDashboardData();
        setupPackageDistributionChart();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // Initialize navigation header views
        View headerView = navigationView.getHeaderView(0);
        navUsername = headerView.findViewById(R.id.username);
        navEmail = headerView.findViewById(R.id.email);
        navProfileImage = headerView.findViewById(R.id.imageView);

        // Initialize TextViews
        totalRevenueText = findViewById(R.id.totalRevenueText);
        totalPackagesText = findViewById(R.id.totalPackagesText);
        successRateText = findViewById(R.id.successRateText);
        activePackagesCount = findViewById(R.id.activePackagesCount);
        deliveredPackagesCount = findViewById(R.id.deliveredPackagesCount);
        returnedPackagesCount = findViewById(R.id.returnedPackagesCount);
        allCountText = findViewById(R.id.allCountText);
        revenueGrowthText = findViewById(R.id.revenueGrowthText);
        packagesGrowthText = findViewById(R.id.packagesGrowthText);
        returnRateText = findViewById(R.id.returnRateText);

        // Progress bar and chart
        activeProgressBar = findViewById(R.id.activeProgressBar);
        packageDistributionChart = findViewById(R.id.packageDistributionChart);

        //Search action Button
        MaterialButton trackButton= findViewById(R.id.trackButton);

        // Main action button
        MaterialButton createPickupButton = findViewById(R.id.createPickupButton);

        // Quick action cards
        activePackagesCard = findViewById(R.id.activePackagesCard);
        deliveredPackagesCard = findViewById(R.id.deliveredPackagesCard);
        returnedPackagesCard = findViewById(R.id.returnedPackagesCard);
        allPackagesCard = findViewById(R.id.allPackagesCard);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Set click listeners
        createPickupButton.setOnClickListener(v ->
                startActivity(new Intent(this, CreatePickupActivity.class)));

        trackButton.setOnClickListener(view ->
                startActivity(new Intent(this, TrackingActivity.class)));

        activePackagesCard.setOnClickListener(v ->
                startActivityWithPackageType("active"));

        deliveredPackagesCard.setOnClickListener(v ->
                startActivityWithPackageType("delivered"));

        returnedPackagesCard.setOnClickListener(v ->
                startActivityWithPackageType("returned"));

        allPackagesCard.setOnClickListener(v ->
                startActivityWithPackageType("all"));
    }

    private void loadMerchantData() {
        // Get merchant data from SharedPreferences (your existing system)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userJson = prefs.getString(USER_KEY, null);

        if (userJson != null) {
            try {
                Map<String, String> userData = new Gson().fromJson(userJson, Map.class);
                String businessName = userData.get("name");
                String email = userData.get("email");
                String role = userData.get("role");

                // Update navigation header with real data
                if (businessName != null && !businessName.isEmpty()) {
                    navUsername.setText(businessName);
                } else {
                    navUsername.setText("Merchant User");
                }

                if (email != null && !email.isEmpty()) {
                    navEmail.setText(email);
                } else {
                    // Fallback to Firebase Auth email
                    if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                        navEmail.setText(mAuth.getCurrentUser().getEmail());
                    } else {
                        navEmail.setText("merchant@1logistics.site");
                    }
                }

                // Update toolbar title with merchant name
                if (getSupportActionBar() != null && businessName != null) {
                    getSupportActionBar().setTitle(businessName + ", Welcome");
                }

                // Also load from Firestore for additional data (optional)
                loadMerchantDataFromFirestore();

            } catch (Exception e) {
                setDefaultNavHeader();
                Log.e("MerchantDashboard", "Error parsing user data: " + e.getMessage());
            }
        } else {
            setDefaultNavHeader();
        }
    }

    private void loadMerchantDataFromFirestore() {
        // Optional: Load additional merchant data from Firestore
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid != null) {
            db.collection("Merchants").document(currentUserUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String businessName = documentSnapshot.getString("businessName");
                            String email = documentSnapshot.getString("email");
                            String pickupLocation = documentSnapshot.getString("pickupLocation");

                            // Update with Firestore data if available
                            if (businessName != null && !businessName.isEmpty()) {
                                navUsername.setText(businessName);

                                // Update SharedPreferences with latest data
                                updateSharedPreferences(businessName, email, pickupLocation);
                            }

                            if (email != null && !email.isEmpty()) {
                                navEmail.setText(email);

                                // Update SharedPreferences with latest data
                                updateSharedPreferences(businessName, email, pickupLocation);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MerchantDashboard", "Error loading merchant data from Firestore: " + e.getMessage());
                    });
        }
    }

    private String getCurrentUserUid() {
        // Try Firebase Auth first
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }

        // Try SharedPreferences as fallback
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userJson = prefs.getString(USER_KEY, null);
        if (userJson != null) {
            try {
                Map<String, String> userData = new Gson().fromJson(userJson, Map.class);
                return userData.get("uid");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void updateSharedPreferences(String businessName, String email, String pickupLocation) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userJson = prefs.getString(USER_KEY, null);

        if (userJson != null) {
            try {
                Map<String, String> userData = new Gson().fromJson(userJson, Map.class);

                // Update with latest data
                if (businessName != null) {
                    userData.put("name", businessName);
                }
                if (email != null) {
                    userData.put("email", email);
                }
                if (pickupLocation != null) {
                    userData.put("pickupLocation", pickupLocation);
                }

                // Save back to SharedPreferences
                String updatedUserJson = new Gson().toJson(userData);
                prefs.edit().putString(USER_KEY, updatedUserJson).apply();

            } catch (Exception e) {
                Log.e("MerchantDashboard", "Error updating SharedPreferences: " + e.getMessage());
            }
        }
    }

    private void setDefaultNavHeader() {
        // Set default values from Firebase Auth or fallback
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            String displayName = mAuth.getCurrentUser().getDisplayName();

            if (email != null) {
                navEmail.setText(email);
            } else {
                navEmail.setText("merchant@1logistics.site");
            }

            if (displayName != null && !displayName.isEmpty()) {
                navUsername.setText(displayName);
            } else {
                navUsername.setText("Merchant User");
            }
        } else {
            // Ultimate fallback
            navUsername.setText("Merchant User");
            navEmail.setText("merchant@1logistics.site");
        }
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                // Already on dashboard

            } else if (id == R.id.nav_active_packages) {
                startActivityWithPackageType("active");

            } else if (id == R.id.nav_delivered_packages) {
                startActivityWithPackageType("delivered");

            } else if (id == R.id.nav_returned_packages) {
                startActivityWithPackageType("returned");

            } else if (id == R.id.nav_all_packages) {
                startActivityWithPackageType("all");

            } else if (id == R.id.nav_merchants) {
                startActivity(new Intent(this, AllMerchantsActivity.class));

            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, MerchantReportsActivity.class));

            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));

            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));

            } else if (id == R.id.nav_logout) {
                logout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }


    private void setupPackageDistributionChart() {
        packageDistributionChart.getDescription().setEnabled(false);
        packageDistributionChart.setDrawHoleEnabled(true);
        packageDistributionChart.setHoleColor(Color.WHITE);
        packageDistributionChart.setTransparentCircleColor(Color.WHITE);
        packageDistributionChart.setTransparentCircleAlpha(110);
        packageDistributionChart.setHoleRadius(58f);
        packageDistributionChart.setTransparentCircleRadius(61f);
        packageDistributionChart.setDrawCenterText(true);
        packageDistributionChart.setRotationAngle(0);
        packageDistributionChart.setRotationEnabled(true);
        packageDistributionChart.setHighlightPerTapEnabled(true);
    }

    private void startActivityWithPackageType(String packageType) {
        Intent intent = new Intent(this, AllPackagesActivity.class);
        intent.putExtra("packageType", packageType);
        intent.putExtra("merchantId", merchantId); // Limit to merchant's packages
        startActivity(intent);
    }

    private void loadDashboardData() {
        loadPackageStatistics();
        loadRevenueData();
        loadMonthlyComparison();
    }

    private void loadPackageStatistics() {
        // Load all packages count for this merchant
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalPackages = queryDocumentSnapshots.size();

                    // Reset counters
                    activePackages = 0;
                    deliveredPackages = 0;
                    returnedPackages = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        String statusCategory = document.getString("statusCategory");

                        if ("Delivered".equals(status)) {
                            deliveredPackages++;
                        } else if ("Returned".equals(status)) {
                            returnedPackages++;
                        } else if ("Active".equals(statusCategory)) {
                            activePackages++;
                        }
                    }

                    updatePackageCountsWithAnimation();
                    calculateSuccessRate();
                    updatePackageDistributionChart();
                    updateProgressBars();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load package statistics");
                });
    }

    private void loadRevenueData() {
        // Calculate total revenue from ALL packages for this merchant
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalRevenue = 0.0;
                    monthlyRevenue = 0.0;
                    lastMonthRevenue = 0.0;

                    Calendar currentMonth = Calendar.getInstance();
                    Calendar lastMonth = Calendar.getInstance();
                    lastMonth.add(Calendar.MONTH, -1);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double codPrice = document.getDouble("codPrice");
                        if (codPrice != null) {
                            totalRevenue += codPrice;

                            // Check if package is from current month
                            if (document.getTimestamp("createdDate") != null) {
                                Calendar packageDate = Calendar.getInstance();
                                packageDate.setTime(document.getTimestamp("createdDate").toDate());

                                if (packageDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                                        packageDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)) {
                                    monthlyRevenue += codPrice;
                                } else if (packageDate.get(Calendar.MONTH) == lastMonth.get(Calendar.MONTH) &&
                                        packageDate.get(Calendar.YEAR) == lastMonth.get(Calendar.YEAR)) {
                                    lastMonthRevenue += codPrice;
                                }
                            }
                        }
                    }

                    updateRevenueWithAnimation();
                    updateRevenueGrowth();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load revenue data");
                });
    }

    private void loadMonthlyComparison() {
        // This would compare current month vs last month performance
        // For now, we're calculating it in loadRevenueData()
    }

    private void updatePackageCountsWithAnimation() {
        animateCount(totalPackagesText, totalPackages);
        animateCount(activePackagesCount, activePackages);
        animateCount(deliveredPackagesCount, deliveredPackages);
        animateCount(allCountText, totalPackages);
        returnedPackagesCount.setText(returnedPackages + " returned");

        // Update packages growth (simplified)
        packagesGrowthText.setText("+12% this month");
    }

    private void updateRevenueWithAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) totalRevenue);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            totalRevenueText.setText(String.format("৳%,.0f", value));
        });
        animator.start();
    }

    private void updateRevenueGrowth() {
        if (lastMonthRevenue > 0) {
            double growthPercentage = ((monthlyRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;
            String growthText = String.format(Locale.getDefault(), "%.1f%% this month", growthPercentage);
            revenueGrowthText.setText(growthText);
            revenueGrowthText.setTextColor(growthPercentage >= 0 ?
                    getColor(R.color.success_color) : getColor(R.color.danger_color));
        } else {
            revenueGrowthText.setText("New month");
            revenueGrowthText.setTextColor(getColor(R.color.info_color));
        }
    }

    private void calculateSuccessRate() {
        int totalCompleted = deliveredPackages + returnedPackages;
        if (totalCompleted > 0) {
            double successRate = (deliveredPackages * 100.0) / totalCompleted;
            double returnRate = (returnedPackages * 100.0) / totalCompleted;

            animateSuccessRate(successRate);
            returnRateText.setText(String.format(Locale.getDefault(), "%.1f%%", returnRate));
        } else {
            successRateText.setText("0%");
            returnRateText.setText("0%");
        }
    }

    private void updateProgressBars() {
        // Update active progress bar
        int activePercentage = totalPackages > 0 ? (activePackages * 100) / totalPackages : 0;
        animateProgress(activeProgressBar, activePercentage);
    }

    private void updatePackageDistributionChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (deliveredPackages > 0) {
            entries.add(new PieEntry(deliveredPackages, "Delivered"));
        }
        if (activePackages > 0) {
            entries.add(new PieEntry(activePackages, "Active"));
        }
        if (returnedPackages > 0) {
            entries.add(new PieEntry(returnedPackages, "Returned"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Package Status");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Colors for slices
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4CAF50")); // Green for delivered
        colors.add(Color.parseColor("#FF9800")); // Orange for active
        colors.add(Color.parseColor("#F44336")); // Red for returned
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        packageDistributionChart.setData(data);
        packageDistributionChart.invalidate();
    }

    private void animateSuccessRate(double targetRate) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) targetRate);
        animator.setDuration(2000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            successRateText.setText(String.format(Locale.getDefault(), "%.1f%%", value));
        });
        animator.start();
    }

    private void animateCount(final TextView textView, final int targetValue) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetValue);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            textView.setText(String.valueOf(animation.getAnimatedValue()));
        });
        animator.start();
    }

    private void animateProgress(final ProgressBar progressBar, final int targetProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetProgress);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            progressBar.setProgress((Integer) animation.getAnimatedValue());
        });
        animator.start();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.merchant_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_refresh) {
            loadDashboardData();
            showToast("Refreshing data...");
            return true;
        } else if (id == R.id.menu_reports) {
            startActivity(new Intent(this, MerchantReportsActivity.class));
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
package com.softcraft.a1logistics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    // TextViews
    private TextView totalRevenueText, totalPackagesText, successRateText;
    private TextView activePackagesCount, deliveredPackagesCount, returnedPackagesCount;
    private TextView revenueGrowthText, packagesGrowthText, returnRateText, totalMerchantsText;

    // Charts
    private LineChart revenueChart;
    private PieChart packageDistributionChart;

    // Progress bars
    private ProgressBar activeProgressBar;

    // Navigation Header Views
    private TextView navUsername, navEmail;
    private ImageView navProfileImage;

    // Statistics
    private int totalPackages = 0;
    private int activePackages = 0;
    private int deliveredPackages = 0;
    private int returnedPackages = 0;
    private int totalMerchants = 0;
    private double totalRevenue = 0.0;
    private double monthlyRevenue = 0.0;
    private double lastMonthRevenue = 0.0;

    // SharedPreferences
    private static final String PREFS_NAME = "A1LogisticsPrefs";
    private static final String USER_KEY = "currentUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupNavigation();
        loadAdminData(); // Load real admin data
        loadDashboardData();
        setupCharts();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

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
        revenueGrowthText = findViewById(R.id.revenueGrowthText);
        packagesGrowthText = findViewById(R.id.packagesGrowthText);
        returnRateText = findViewById(R.id.returnRateText);
        totalMerchantsText = findViewById(R.id.totalMerchantsText);

        // Charts
        revenueChart = findViewById(R.id.revenueChart);
        packageDistributionChart = findViewById(R.id.packageDistributionChart);

        // Progress bars
        activeProgressBar = findViewById(R.id.activeProgressBar);

        // Setup toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void loadAdminData() {
        // Get admin data from SharedPreferences (your existing system)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userJson = prefs.getString(USER_KEY, null);

        if (userJson != null) {
            try {
                Map<String, String> userData = new Gson().fromJson(userJson, Map.class);
                String adminName = userData.get("name");
                String email = userData.get("email");
                String role = userData.get("role");

                // Update navigation header with real data
                if (adminName != null && !adminName.isEmpty()) {
                    navUsername.setText(adminName);
                } else {
                    navUsername.setText("Admin User");
                }

                if (email != null && !email.isEmpty()) {
                    navEmail.setText(email);
                } else {
                    // Fallback to Firebase Auth email
                    if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                        navEmail.setText(mAuth.getCurrentUser().getEmail());
                    } else {
                        navEmail.setText("admin@1logistics.site");
                    }
                }

                // Update toolbar title with admin name
                if (getSupportActionBar() != null && adminName != null) {
                    getSupportActionBar().setTitle(adminName + ", Welcome");
                }

                // Also load from Firestore for additional data (optional)
                loadAdminDataFromFirestore();

            } catch (Exception e) {
                setDefaultNavHeader();
                Log.e("AdminDashboard", "Error parsing user data: " + e.getMessage());
            }
        } else {
            setDefaultNavHeader();
        }
    }

    private void loadAdminDataFromFirestore() {
        // Optional: Load additional admin data from Firestore
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid != null) {
            db.collection("Admins").document(currentUserUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String adminName = documentSnapshot.getString("adminName");
                            String email = documentSnapshot.getString("email");

                            // Update with Firestore data if available
                            if (adminName != null && !adminName.isEmpty()) {
                                navUsername.setText(adminName);

                                // Update SharedPreferences with latest data
                                updateSharedPreferences(adminName, email);
                            }

                            if (email != null && !email.isEmpty()) {
                                navEmail.setText(email);

                                // Update SharedPreferences with latest data
                                updateSharedPreferences(adminName, email);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminDashboard", "Error loading admin data from Firestore: " + e.getMessage());
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

    private void updateSharedPreferences(String adminName, String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userJson = prefs.getString(USER_KEY, null);

        if (userJson != null) {
            try {
                Map<String, String> userData = new Gson().fromJson(userJson, Map.class);

                // Update with latest data
                if (adminName != null) {
                    userData.put("name", adminName);
                }
                if (email != null) {
                    userData.put("email", email);
                }

                // Save back to SharedPreferences
                String updatedUserJson = new Gson().toJson(userData);
                prefs.edit().putString(USER_KEY, updatedUserJson).apply();

            } catch (Exception e) {
                Log.e("AdminDashboard", "Error updating SharedPreferences: " + e.getMessage());
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
                navEmail.setText("admin@1logistics.site");
            }

            if (displayName != null && !displayName.isEmpty()) {
                navUsername.setText(displayName);
            } else {
                navUsername.setText("Admin User");
            }
        } else {
            // Ultimate fallback
            navUsername.setText("Admin User");
            navEmail.setText("admin@1logistics.site");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
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
            startActivity(new Intent(this, ReportsActivity.class));
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                startActivity(new Intent(this, ReportsActivity.class));

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
    private void startActivityWithPackageType(String type) {
        Intent intent = new Intent(this, AllPackagesActivity.class);
        intent.putExtra("packageType", type);
        startActivity(intent);
    }


    private void loadDashboardData() {
        loadPackageStatistics();
        loadRevenueData();
        loadMerchantsCount();
        loadMonthlyRevenueComparison();
    }

    private void loadPackageStatistics() {
        db.collection("PickupRequests")
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
                });
    }

    private void loadRevenueData() {
        db.collection("PickupRequests")
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
                    updateRevenueChart();
                });
    }

    private void loadMerchantsCount() {
        db.collection("Merchants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalMerchants = queryDocumentSnapshots.size();
                    animateCount(totalMerchantsText, totalMerchants);
                });
    }

    private void loadMonthlyRevenueComparison() {
        // This would be implemented to compare current month vs last month
        // For now, we're calculating it in loadRevenueData()
    }

    private void updatePackageCountsWithAnimation() {
        animateCount(totalPackagesText, totalPackages);
        animateCount(activePackagesCount, activePackages);
        animateCount(deliveredPackagesCount, deliveredPackages);
        returnedPackagesCount.setText(returnedPackages + " returned");

        // Update active progress bar
        int activePercentage = totalPackages > 0 ? (activePackages * 100) / totalPackages : 0;
        animateProgress(activeProgressBar, activePercentage);
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

    private void setupCharts() {
        setupRevenueChart();
        setupPackageDistributionChart();
    }

    private void setupRevenueChart() {
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setTouchEnabled(true);
        revenueChart.setDragEnabled(true);
        revenueChart.setScaleEnabled(true);
        revenueChart.setPinchZoom(true);
        revenueChart.setDrawGridBackground(false);

        // X-axis setup
        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(6);

        // Y-axis setup
        YAxis leftAxis = revenueChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("৳%.0f", value);
            }
        });

        revenueChart.getAxisRight().setEnabled(false);
        revenueChart.getLegend().setEnabled(false);
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

        Legend l = packageDistributionChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);
    }

    private void updateRevenueChart() {
        // Sample data - in real app, you'd fetch monthly data
        ArrayList<Entry> values = new ArrayList<>();
        values.add(new Entry(0, 45000));
        values.add(new Entry(1, 52000));
        values.add(new Entry(2, 48000));
        values.add(new Entry(3, 61000));
        values.add(new Entry(4, 75000));
        values.add(new Entry(5, (float) monthlyRevenue));

        LineDataSet set1 = new LineDataSet(values, "Monthly Revenue");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(Color.parseColor("#2196F3"));
        set1.setCircleColor(Color.parseColor("#2196F3"));
        set1.setLineWidth(2f);
        set1.setCircleRadius(3f);
        set1.setFillAlpha(65);
        set1.setFillColor(Color.parseColor("#2196F3"));
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);
        set1.setDrawValues(false);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        LineData data = new LineData(dataSets);
        revenueChart.setData(data);
        revenueChart.invalidate();
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
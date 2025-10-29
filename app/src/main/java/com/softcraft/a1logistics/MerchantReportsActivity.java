package com.softcraft.a1logistics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.Menu;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MerchantReportsActivity extends BaseActivity {

    private FirebaseFirestore db;
    private String merchantId;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MerchantReportsPagerAdapter pagerAdapter;

    // Summary statistics
    private TextView totalRevenueText, totalPackagesText, successRateText, avgDeliveryTimeText;
    private TextView monthlyGrowthText, returnRateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_reports);

        Map<String, String> user = getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        merchantId = user.get("uid");
        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupViewPager();
        loadSummaryStatistics();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        totalRevenueText = findViewById(R.id.totalRevenueText);
        totalPackagesText = findViewById(R.id.totalPackagesText);
        successRateText = findViewById(R.id.successRateText);
        avgDeliveryTimeText = findViewById(R.id.avgDeliveryTimeText);
        monthlyGrowthText = findViewById(R.id.monthlyGrowthText);
        returnRateText = findViewById(R.id.returnRateText);

        // Setup toolbar - EXACTLY LIKE REPORTS ACTIVITY
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Analytics & Reports");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showDateFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDateFilterDialog() {
        String[] filterOptions = {"Last 7 Days", "Last 30 Days", "Last 3 Months", "Custom Range"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Filter by Date Range");
        builder.setItems(filterOptions, (dialog, which) -> {
            switch (which) {
                case 0:
                    applyDateFilter(7);
                    break;
                case 1:
                    applyDateFilter(30);
                    break;
                case 2:
                    applyDateFilter(90);
                    break;
                case 3:
                    showCustomDateRangeDialog();
                    break;
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void applyDateFilter(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);

        // Reload data with date filter
        loadFilteredData(calendar.getTime());
        android.widget.Toast.makeText(this, "Showing data from last " + days + " days",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void loadFilteredData(java.util.Date startDate) {
        // Implement your filtered data loading here
        loadSummaryStatistics(); // Reload for now
    }

    private void showCustomDateRangeDialog() {
        android.widget.Toast.makeText(this, "Custom date range feature",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setupViewPager() {
        pagerAdapter = new MerchantReportsPagerAdapter(this, merchantId);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Overview");
                    break;
                case 1:
                    tab.setText("Revenue");
                    break;
                case 2:
                    tab.setText("Packages");
                    break;
            }
        }).attach();
    }

    private void loadSummaryStatistics() {
        loadRevenueStats();
        loadPackageStats();
        loadPerformanceStats();
    }

    private void loadRevenueStats() {
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalRevenue = 0;
                    double monthlyRevenue = 0;
                    double lastMonthRevenue = 0;

                    Calendar currentMonth = Calendar.getInstance();
                    Calendar lastMonth = Calendar.getInstance();
                    lastMonth.add(Calendar.MONTH, -1);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double codPrice = document.getDouble("codPrice");
                        if (codPrice != null) {
                            totalRevenue += codPrice;

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

                    animateCount(totalRevenueText, totalRevenue, "৳%,.0f");

                    // Calculate growth
                    if (lastMonthRevenue > 0) {
                        double growth = ((monthlyRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;
                        String growthText = String.format(Locale.getDefault(), "%.1f%% from last month", growth);
                        monthlyGrowthText.setText(growthText);
                        monthlyGrowthText.setTextColor(growth >= 0 ?
                                ContextCompat.getColor(this, R.color.success_color) :
                                ContextCompat.getColor(this, R.color.danger_color));
                    }
                });
    }

    private void loadPackageStats() {
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalPackages = queryDocumentSnapshots.size();
                    int delivered = 0;
                    int returned = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        if ("Delivered".equals(status)) delivered++;
                        if ("Returned".equals(status)) returned++;
                    }

                    animateCount(totalPackagesText, totalPackages, "%,.0f");

                    // Calculate success rate
                    if (delivered + returned > 0) {
                        double successRate = (delivered * 100.0) / (delivered + returned);
                        double returnRate = (returned * 100.0) / (delivered + returned);

                        animateSuccessRate(successRateText, successRate);
                        animateSuccessRate(returnRateText, returnRate);
                    }
                });
    }

    private void loadPerformanceStats() {
        // Calculate average delivery time (in hours)
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .whereEqualTo("status", "Delivered")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalDeliveryTime = 0;
                    int count = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (document.getTimestamp("createdDate") != null &&
                                document.getTimestamp("lastUpdate") != null) {

                            long created = document.getTimestamp("createdDate").toDate().getTime();
                            long delivered = document.getTimestamp("lastUpdate").toDate().getTime();
                            long deliveryTimeHours = (delivered - created) / (1000 * 60 * 60);

                            totalDeliveryTime += deliveryTimeHours;
                            count++;
                        }
                    }

                    if (count > 0) {
                        double avgTime = totalDeliveryTime / (double) count;
                        avgDeliveryTimeText.setText(String.format(Locale.getDefault(), "%.1f hours", avgTime));
                    } else {
                        avgDeliveryTimeText.setText("N/A");
                    }
                });
    }

    private void animateCount(TextView textView, double value, String format) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) value);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            String formattedText = String.format(Locale.getDefault(), format, animatedValue);
            textView.setText(formattedText);
        });
        animator.start();
    }

    private void animateSuccessRate(TextView textView, double value) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) value);
        animator.setDuration(2000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            textView.setText(String.format(Locale.getDefault(), "%.1f%%", animatedValue));
        });
        animator.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
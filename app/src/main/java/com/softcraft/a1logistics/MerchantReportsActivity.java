package com.softcraft.a1logistics;

import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class MerchantReportsActivity extends BaseActivity {

    private FirebaseFirestore db;
    private String merchantId;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    // Summary statistics
    private TextView totalRevenueText, totalPackagesText, successRateText, avgDeliveryTimeText;
    private TextView monthlyGrowthText, returnRateText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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

        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Analytics & Reports");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // No menu for settings
        return false;
    }
    private void setupViewPager() {
        MerchantReportsPagerAdapter pagerAdapter = new MerchantReportsPagerAdapter(this, merchantId);
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

                    totalRevenueText.setText(String.format("৳%,.0f", totalRevenue));

                    // Calculate growth
                    if (lastMonthRevenue > 0) {
                        double growth = ((monthlyRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;
                        String growthText = String.format(Locale.getDefault(), "+%.1f%%", growth);
                        monthlyGrowthText.setText(growthText);
                        monthlyGrowthText.setTextColor(growth >= 0 ?
                                getColor(R.color.success_color) : getColor(R.color.danger_color));
                    } else {
                        monthlyGrowthText.setText("New month");
                        monthlyGrowthText.setTextColor(getColor(R.color.info_color));
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

                    totalPackagesText.setText(String.valueOf(totalPackages));

                    // Calculate success rate
                    if (delivered + returned > 0) {
                        double successRate = (delivered * 100.0) / (delivered + returned);
                        double returnRate = (returned * 100.0) / (delivered + returned);

                        successRateText.setText(String.format(Locale.getDefault(), "%.1f%%", successRate));
                        returnRateText.setText(String.format(Locale.getDefault(), "%.1f%%", returnRate));
                    }
                });
    }

    private void loadPerformanceStats() {
        // Calculate average delivery time (simplified)
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
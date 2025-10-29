package com.softcraft.a1logistics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
public class ReportsActivity extends BaseActivity {

    private FirebaseFirestore db;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ReportsPagerAdapter pagerAdapter;

    // Summary statistics
    private TextView totalRevenueText, totalPackagesText, successRateText, avgDeliveryTimeText;
    private TextView monthlyGrowthText, topMerchantText, peakDayText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

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
        topMerchantText = findViewById(R.id.topMerchantText);
        peakDayText = findViewById(R.id.peakDayText);

        // Setup toolbar - EXACTLY AS YOU HAD IT
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Analytics & Reports");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // NEW FEATURE: Quick date filter
    private void setupQuickFilter() {
        // Add filter button to toolbar
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
        // This would modify your existing Firestore queries to include date filtering
    }

    private void showCustomDateRangeDialog() {
        // Implement custom date range picker
        android.widget.Toast.makeText(this, "Custom date range feature",
                android.widget.Toast.LENGTH_SHORT).show();
    }
    private void setupViewPager() {
        pagerAdapter = new ReportsPagerAdapter(this);
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
                case 3:
                    tab.setText("Merchants");
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
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalPackages = queryDocumentSnapshots.size();
                    int delivered = 0;
                    int returned = 0;

                    Map<String, Integer> merchantPackageCount = new HashMap<>();
                    Map<String, Integer> dayCount = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        String merchantName = document.getString("merchantName");

                        if ("Delivered".equals(status)) delivered++;
                        if ("Returned".equals(status)) returned++;

                        // Count packages by merchant
                        if (merchantName != null) {
                            merchantPackageCount.put(merchantName,
                                    merchantPackageCount.getOrDefault(merchantName, 0) + 1);
                        }

                        // Count packages by day
                        if (document.getTimestamp("createdDate") != null) {
                            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                            String day = dayFormat.format(document.getTimestamp("createdDate").toDate());
                            dayCount.put(day, dayCount.getOrDefault(day, 0) + 1);
                        }
                    }

                    // FIX: Changed from "%,d" to "%,.0f"
                    animateCount(totalPackagesText, totalPackages, "%,.0f");

                    // Calculate success rate
                    if (delivered + returned > 0) {
                        double successRate = (delivered * 100.0) / (delivered + returned);
                        animateSuccessRate(successRateText, successRate);
                    }

                    // Find top merchant
                    String topMerchant = findTopPerformer(merchantPackageCount);
                    topMerchantText.setText(topMerchant != null ? topMerchant : "N/A");

                    // Find peak day
                    String peakDay = findTopPerformer(dayCount);
                    peakDayText.setText(peakDay != null ? peakDay : "N/A");
                });
    }

    private void loadPerformanceStats() {
        // Calculate average delivery time (in hours)
        db.collection("PickupRequests")
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

    private String findTopPerformer(Map<String, Integer> data) {
        return data.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void animateCount(TextView textView, double value, String format) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) value);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();

            // Ensure we're using the correct format for float values
            String formattedText;
            if (format.contains("%d")) {
                // Replace %d with %.0f for integer representation of float
                String fixedFormat = format.replace("%d", "%.0f");
                formattedText = String.format(Locale.getDefault(), fixedFormat, animatedValue);
            } else {
                formattedText = String.format(Locale.getDefault(), format, animatedValue);
            }
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

    // Chart setup methods for different fragments
    public static void setupRevenueChart(LineChart chart, List<Double> monthlyRevenue) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.WHITE);

        ArrayList<Entry> values = new ArrayList<>();
        for (int i = 0; i < monthlyRevenue.size(); i++) {
            values.add(new Entry(i, monthlyRevenue.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(values, "Monthly Revenue");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("৳%.0f", value);
            }
        });

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    public static void setupPackageDistributionChart(PieChart chart, int delivered, int active, int returned) {
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleColor(Color.WHITE);
        chart.setHoleRadius(45f);
        chart.setTransparentCircleRadius(50f);
        chart.setDrawCenterText(true);
        chart.setCenterText("Packages");
        chart.setCenterTextSize(14f);

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (delivered > 0) entries.add(new PieEntry(delivered, "Delivered"));
        if (active > 0) entries.add(new PieEntry(active, "Active"));
        if (returned > 0) entries.add(new PieEntry(returned, "Returned"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4CAF50"));
        colors.add(Color.parseColor("#FF9800"));
        colors.add(Color.parseColor("#F44336"));
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        chart.setData(data);
        chart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
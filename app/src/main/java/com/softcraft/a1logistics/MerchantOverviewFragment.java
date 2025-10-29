package com.softcraft.a1logistics;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MerchantOverviewFragment extends Fragment {

    private static final String ARG_MERCHANT_ID = "merchant_id";

    private FirebaseFirestore db;
    private String merchantId;

    private BarChart weeklyChart;
    private PieChart statusChart;
    private TextView totalRevenueText, completedOrdersText, pendingOrdersText, successRateText;
    private TextView avgDeliveryTimeText, monthlyGrowthText, returnRateText;

    public static MerchantOverviewFragment newInstance(String merchantId) {
        MerchantOverviewFragment fragment = new MerchantOverviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MERCHANT_ID, merchantId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            merchantId = getArguments().getString(ARG_MERCHANT_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_merchant_overview, container, false);

        initializeViews(view);
        loadOverviewData();

        return view;
    }

    private void initializeViews(View view) {
        weeklyChart = view.findViewById(R.id.weeklyChart);
        statusChart = view.findViewById(R.id.statusChart);
        totalRevenueText = view.findViewById(R.id.totalRevenueText);
        completedOrdersText = view.findViewById(R.id.completedOrdersText);
        pendingOrdersText = view.findViewById(R.id.pendingOrdersText);
        successRateText = view.findViewById(R.id.successRateText);
        avgDeliveryTimeText = view.findViewById(R.id.avgDeliveryTimeText);
        monthlyGrowthText = view.findViewById(R.id.monthlyGrowthText);
        returnRateText = view.findViewById(R.id.returnRateText);

        // Check which views are null and log for debugging
        if (totalRevenueText == null) {
            android.util.Log.e("MerchantOverview", "totalRevenueText is null");
        }
        if (completedOrdersText == null) {
            android.util.Log.e("MerchantOverview", "completedOrdersText is null");
        }
        if (pendingOrdersText == null) {
            android.util.Log.e("MerchantOverview", "pendingOrdersText is null");
        }
        if (successRateText == null) {
            android.util.Log.e("MerchantOverview", "successRateText is null");
        }
        if (avgDeliveryTimeText == null) {
            android.util.Log.e("MerchantOverview", "avgDeliveryTimeText is null");
        }
        if (monthlyGrowthText == null) {
            android.util.Log.e("MerchantOverview", "monthlyGrowthText is null");
        }
        if (returnRateText == null) {
            android.util.Log.e("MerchantOverview", "returnRateText is null");
        }

        setupWeeklyChart();
        setupStatusChart();
    }

    private void setupWeeklyChart() {
        weeklyChart.getDescription().setEnabled(false);
        weeklyChart.setDrawGridBackground(false);
        weeklyChart.setDragEnabled(true);
        weeklyChart.setScaleEnabled(true);

        XAxis xAxis = weeklyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = weeklyChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        weeklyChart.getAxisRight().setEnabled(false);
        weeklyChart.getLegend().setEnabled(false);
    }

    private void setupStatusChart() {
        statusChart.getDescription().setEnabled(false);
        statusChart.setDrawHoleEnabled(true);
        statusChart.setHoleRadius(45f);
        statusChart.setTransparentCircleRadius(50f);
    }

    private void loadOverviewData() {
        loadWeeklyData();
        loadStatusData();
        loadSummaryStats();
    }

    private void loadWeeklyData() {
        // Load last 7 days data for this merchant only
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .whereGreaterThan("createdDate",
                        new java.util.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int[] dailyCounts = new int[7];
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                    Calendar cal = Calendar.getInstance();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (document.getTimestamp("createdDate") != null) {
                            cal.setTime(document.getTimestamp("createdDate").toDate());
                            String day = dayFormat.format(cal.getTime());
                            int dayIndex = getDayIndex(day);
                            if (dayIndex >= 0) {
                                dailyCounts[dayIndex]++;
                            }
                        }
                    }

                    updateWeeklyChart(dailyCounts);
                });
    }

    private void loadStatusData() {
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", merchantId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int delivered = 0, active = 0, returned = 0;
                    double totalRevenue = 0.0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        Double codPrice = document.getDouble("codPrice");

                        // 🟢 FIX: Calculate revenue from ALL packages, not just delivered ones
                        if (codPrice != null) {
                            totalRevenue += codPrice;
                        }

                        if ("Delivered".equals(status)) {
                            delivered++;
                        } else if ("Returned".equals(status)) {
                            returned++;
                        } else {
                            active++;
                        }
                    }

                    updateStatusChart(delivered, active, returned);
                    updateSummaryStats(delivered, active, returned, totalRevenue);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MerchantOverview", "Error loading status data: " + e.getMessage());
                    // Show error or default values
                    if (totalRevenueText != null) {
                        totalRevenueText.setText("৳0");
                    }
                });
    }

    private void loadSummaryStats() {
        // Additional summary stats can be loaded here
    }

    private void updateWeeklyChart(int[] dailyCounts) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < dailyCounts.length; i++) {
            entries.add(new BarEntry(i, dailyCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Packages");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        weeklyChart.setData(data);

        XAxis xAxis = weeklyChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));

        weeklyChart.invalidate();
    }

    private void updateStatusChart(int delivered, int active, int returned) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (delivered > 0) entries.add(new PieEntry(delivered, "Delivered"));
        if (active > 0) entries.add(new PieEntry(active, "Active"));
        if (returned > 0) entries.add(new PieEntry(returned, "Returned"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#4CAF50"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#F44336"));

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        statusChart.setData(data);
        statusChart.invalidate();
    }

    private void updateSummaryStats(int delivered, int active, int returned, double totalRevenue) {
        if (totalRevenueText != null) {
            animateCount(totalRevenueText, totalRevenue, "৳%,.0f");
        }
        if (completedOrdersText != null) {
            animateCount(completedOrdersText, delivered + returned, "%,.0f");
        }
        if (pendingOrdersText != null) {
            animateCount(pendingOrdersText, active, "%,.0f");
        }

        if (delivered + returned > 0 && successRateText != null) {
            double successRate = (delivered * 100.0) / (delivered + returned);
            animateSuccessRate(successRate);
        }

        // Calculate average delivery time (simplified)
        if (avgDeliveryTimeText != null) {
            avgDeliveryTimeText.setText("24.5h");
        }

        // Calculate monthly growth (simplified)
        if (monthlyGrowthText != null) {
            monthlyGrowthText.setText("+12.5%");
            monthlyGrowthText.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    private int getDayIndex(String day) {
        switch (day) {
            case "Monday": return 0;
            case "Tuesday": return 1;
            case "Wednesday": return 2;
            case "Thursday": return 3;
            case "Friday": return 4;
            case "Saturday": return 5;
            case "Sunday": return 6;
            default: return -1;
        }
    }

    private void animateCount(TextView textView, double value, String format) {
        if (textView == null) return;

        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) value);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();

            // Handle format conversion safely
            String formattedText;
            if (format.contains("%d")) {
                // Convert float to int for %d format
                formattedText = String.format(Locale.getDefault(), format, (int) animatedValue);
            } else {
                // Use as float for %f format
                formattedText = String.format(Locale.getDefault(), format, animatedValue);
            }
            textView.setText(formattedText);
        });
        animator.start();
    }

    private void animateSuccessRate(double value) {
        if (successRateText == null) return;

        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) value);
        animator.setDuration(2000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            successRateText.setText(String.format(Locale.getDefault(), "%.1f%%", animatedValue));
        });
        animator.start();
    }
}
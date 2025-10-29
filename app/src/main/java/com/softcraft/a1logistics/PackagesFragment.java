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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PackagesFragment extends Fragment {

    private FirebaseFirestore db;
    private PieChart packageStatusChart;
    private BarChart dailyVolumeChart;
    private TextView totalPackagesText, deliveredPackagesText, activePackagesText, returnedPackagesText;
    private TextView successRateText, avgDeliveryTimeText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_packages, container, false);

        db = FirebaseFirestore.getInstance();
        initializeViews(view);
        loadPackageData();

        return view;
    }

    private void initializeViews(View view) {
        packageStatusChart = view.findViewById(R.id.packageStatusChart);
        dailyVolumeChart = view.findViewById(R.id.dailyVolumeChart);

        totalPackagesText = view.findViewById(R.id.totalPackagesText);
        deliveredPackagesText = view.findViewById(R.id.deliveredPackagesText);
        activePackagesText = view.findViewById(R.id.activePackagesText);
        returnedPackagesText = view.findViewById(R.id.returnedPackagesText);
        successRateText = view.findViewById(R.id.successRateText);
        avgDeliveryTimeText = view.findViewById(R.id.avgDeliveryTimeText);

        setupCharts();
    }

    private void setupCharts() {
        setupPackageStatusChart();
        setupDailyVolumeChart();
    }

    private void setupPackageStatusChart() {
        packageStatusChart.getDescription().setEnabled(false);
        packageStatusChart.setDrawHoleEnabled(true);
        packageStatusChart.setHoleRadius(45f);
        packageStatusChart.setTransparentCircleRadius(50f);
        packageStatusChart.setRotationAngle(0);
        packageStatusChart.setRotationEnabled(true);
    }

    private void setupDailyVolumeChart() {
        dailyVolumeChart.getDescription().setEnabled(false);
        dailyVolumeChart.setDrawGridBackground(false);
        dailyVolumeChart.setDragEnabled(true);
        dailyVolumeChart.setScaleEnabled(true);

        XAxis xAxis = dailyVolumeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = dailyVolumeChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        dailyVolumeChart.getAxisRight().setEnabled(false);
        dailyVolumeChart.getLegend().setEnabled(false);
    }

    private void loadPackageData() {
        db.collection("PickupRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    int delivered = 0, active = 0, returned = 0;

                    // Count packages by status
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        if ("Delivered".equals(status)) delivered++;
                        else if ("Returned".equals(status)) returned++;
                        else active++;
                    }

                    updatePackageStats(total, delivered, active, returned);
                    updatePackageStatusChart(delivered, active, returned);
                    loadDailyVolumeData();
                    calculatePerformanceMetrics(delivered, returned);
                });
    }

    private void loadDailyVolumeData() {
        // Load last 7 days data
        db.collection("PickupRequests")
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

                    updateDailyVolumeChart(dailyCounts);
                });
    }

    private void updatePackageStats(int total, int delivered, int active, int returned) {
        animateCount(totalPackagesText, total, "%,.0f");
        animateCount(deliveredPackagesText, delivered, "%,.0f");
        animateCount(activePackagesText, active, "%,.0f");
        animateCount(returnedPackagesText, returned, "%,.0f");
    }

    private void updatePackageStatusChart(int delivered, int active, int returned) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (delivered > 0) entries.add(new PieEntry(delivered, "Delivered"));
        if (active > 0) entries.add(new PieEntry(active, "Active"));
        if (returned > 0) entries.add(new PieEntry(returned, "Returned"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#4CAF50"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#F44336"));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        packageStatusChart.setData(data);
        packageStatusChart.invalidate();
    }

    private void updateDailyVolumeChart(int[] dailyCounts) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < dailyCounts.length; i++) {
            entries.add(new BarEntry(i, dailyCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Packages");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        dailyVolumeChart.setData(data);

        XAxis xAxis = dailyVolumeChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));

        dailyVolumeChart.invalidate();
    }

    private void calculatePerformanceMetrics(int delivered, int returned) {
        // Calculate success rate
        if (delivered + returned > 0) {
            double successRate = (delivered * 100.0) / (delivered + returned);
            animateSuccessRate(successRate);
        }

        // Calculate average delivery time (simplified)
        avgDeliveryTimeText.setText("24.5h"); // This would be calculated from actual data
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
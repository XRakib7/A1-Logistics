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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class OverviewFragment extends Fragment {

    private FirebaseFirestore db;
    private BarChart weeklyChart;
    private PieChart statusChart;
    private TextView totalRevenueText, completedOrdersText, pendingOrdersText, successRateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        db = FirebaseFirestore.getInstance();
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

                    updateWeeklyChart(dailyCounts);
                });
    }

    private void loadStatusData() {
        db.collection("PickupRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int delivered = 0, active = 0, returned = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        if ("Delivered".equals(status)) delivered++;
                        else if ("Returned".equals(status)) returned++;
                        else active++;
                    }

                    updateStatusChart(delivered, active, returned);
                    updateSummaryStats(delivered, active, returned);
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
        dataSet.setColors(new int[]{
                Color.parseColor("#4CAF50"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#F44336")
        });

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        statusChart.setData(data);
        statusChart.invalidate();
    }

    private void updateSummaryStats(int delivered, int active, int returned) {
        animateCount(totalRevenueText, calculateTotalRevenue(), "৳%,.0f");
        animateCount(completedOrdersText, delivered + returned, "%,.0f"); // Changed from "%,d"
        animateCount(pendingOrdersText, active, "%,.0f"); // Changed from "%,d"

        if (delivered + returned > 0) {
            double successRate = (delivered * 100.0) / (delivered + returned);
            animateSuccessRate(successRate);
        }
    }

    private double calculateTotalRevenue() {
        // This would be calculated from actual data
        return 125000.0; // Example value
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
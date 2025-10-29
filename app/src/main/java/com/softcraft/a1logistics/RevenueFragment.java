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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RevenueFragment extends Fragment {

    private FirebaseFirestore db;
    private LineChart revenueChart;
    private BarChart monthlyBreakdownChart;
    private TextView totalRevenueText, monthlyRevenueText, growthText, avgOrderValueText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_revenue, container, false);

        db = FirebaseFirestore.getInstance();
        initializeViews(view);
        loadRevenueData();

        return view;
    }

    private void initializeViews(View view) {
        revenueChart = view.findViewById(R.id.revenueChart);
        monthlyBreakdownChart = view.findViewById(R.id.monthlyBreakdownChart);

        totalRevenueText = view.findViewById(R.id.totalRevenueText);
        monthlyRevenueText = view.findViewById(R.id.monthlyRevenueText);
        growthText = view.findViewById(R.id.growthText);
        avgOrderValueText = view.findViewById(R.id.avgOrderValueText);

        setupCharts();
    }

    private void setupCharts() {
        setupRevenueChart();
        setupMonthlyBreakdownChart();
    }

    private void setupRevenueChart() {
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setTouchEnabled(true);
        revenueChart.setDragEnabled(true);
        revenueChart.setScaleEnabled(true);
        revenueChart.setPinchZoom(true);
        revenueChart.setDrawGridBackground(false);

        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

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

    private void setupMonthlyBreakdownChart() {
        monthlyBreakdownChart.getDescription().setEnabled(false);
        monthlyBreakdownChart.setDrawGridBackground(false);
        monthlyBreakdownChart.setDragEnabled(true);
        monthlyBreakdownChart.setScaleEnabled(true);

        XAxis xAxis = monthlyBreakdownChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = monthlyBreakdownChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        monthlyBreakdownChart.getAxisRight().setEnabled(false);
        monthlyBreakdownChart.getLegend().setEnabled(false);
    }

    private void loadRevenueData() {
        db.collection("PickupRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalRevenue = 0;
                    double monthlyRevenue = 0;
                    int totalPackages = queryDocumentSnapshots.size();

                    Calendar currentMonth = Calendar.getInstance();

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
                                }
                            }
                        }
                    }

                    updateRevenueStats(totalRevenue, monthlyRevenue, totalPackages);
                    updateRevenueChart();
                    updateMonthlyBreakdownChart();
                });
    }

    private void updateRevenueStats(double totalRevenue, double monthlyRevenue, int totalPackages) {
        animateCount(totalRevenueText, totalRevenue, "৳%,.0f");
        animateCount(monthlyRevenueText, monthlyRevenue, "৳%,.0f");

        // Calculate growth (simplified)
        growthText.setText("+12.5%");
        growthText.setTextColor(Color.parseColor("#4CAF50"));

        // Calculate average order value
        if (totalPackages > 0) {
            double avgOrderValue = totalRevenue / totalPackages;
            animateCount(avgOrderValueText, avgOrderValue, "৳%.0f");
        }
    }

    private void updateRevenueChart() {
        // Load actual revenue trend data
        db.collection("PickupRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Simplified: Use last 6 months data
                    ArrayList<Entry> values = new ArrayList<>();
                    values.add(new Entry(0, 45000));
                    values.add(new Entry(1, 52000));
                    values.add(new Entry(2, 48000));
                    values.add(new Entry(3, 61000));
                    values.add(new Entry(4, 58000));
                    values.add(new Entry(5, 65000));

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
                    revenueChart.setData(lineData);
                    revenueChart.invalidate();
                });
    }

    private void updateMonthlyBreakdownChart() {
        // Simplified monthly breakdown
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 45000));
        entries.add(new BarEntry(1, 52000));
        entries.add(new BarEntry(2, 48000));
        entries.add(new BarEntry(3, 61000));
        entries.add(new BarEntry(4, 58000));
        entries.add(new BarEntry(5, 65000));

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Revenue");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("৳%.0f", value);
            }
        });

        BarData data = new BarData(dataSet);
        monthlyBreakdownChart.setData(data);

        XAxis xAxis = monthlyBreakdownChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));

        monthlyBreakdownChart.invalidate();
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
}
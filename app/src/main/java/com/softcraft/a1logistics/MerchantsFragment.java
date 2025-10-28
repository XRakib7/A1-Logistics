package com.softcraft.a1logistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MerchantsFragment extends Fragment {

    private FirebaseFirestore db;
    private PieChart merchantRevenueChart;
    private LineChart merchantGrowthChart;
    private TextView totalMerchantsText, activeMerchantsText;
    private TextView avgPackagesPerMerchantText, topMerchantShareText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_merchants, container, false);

        db = FirebaseFirestore.getInstance();
        initializeViews(view);
        loadMerchantData();

        return view;
    }

    private void initializeViews(View view) {
        merchantRevenueChart = view.findViewById(R.id.merchantRevenueChart);
        merchantGrowthChart = view.findViewById(R.id.merchantGrowthChart);

        totalMerchantsText = view.findViewById(R.id.totalMerchantsText);
        activeMerchantsText = view.findViewById(R.id.activeMerchantsText);
        avgPackagesPerMerchantText = view.findViewById(R.id.avgPackagesPerMerchantText);
        topMerchantShareText = view.findViewById(R.id.topMerchantShareText);

        setupCharts();
    }

    private void setupCharts() {
        setupMerchantRevenueChart();
        setupMerchantGrowthChart();
    }

    private void setupMerchantRevenueChart() {
        merchantRevenueChart.getDescription().setEnabled(false);
        merchantRevenueChart.setDrawHoleEnabled(true);
        merchantRevenueChart.setHoleRadius(45f);
        merchantRevenueChart.setTransparentCircleRadius(50f);
    }

    private void setupMerchantGrowthChart() {
        merchantGrowthChart.getDescription().setEnabled(false);
        merchantGrowthChart.setTouchEnabled(true);
        merchantGrowthChart.setDragEnabled(true);
        merchantGrowthChart.setScaleEnabled(true);

        XAxis xAxis = merchantGrowthChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = merchantGrowthChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        merchantGrowthChart.getAxisRight().setEnabled(false);
        merchantGrowthChart.getLegend().setEnabled(false);
    }

    private void loadMerchantData() {
        // Load merchants count
        db.collection("Merchants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalMerchants = queryDocumentSnapshots.size();
                    totalMerchantsText.setText(String.valueOf(totalMerchants));
                    activeMerchantsText.setText(String.valueOf(totalMerchants)); // Assuming all are active

                    loadMerchantPerformance();
                    loadRevenueDistribution();
                    loadGrowthData();
                });
    }

    private void loadMerchantPerformance() {
        db.collection("PickupRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> merchantPackageCount = new HashMap<>();
                    Map<String, Double> merchantRevenue = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String merchantName = document.getString("merchantName");
                        Double codPrice = document.getDouble("codPrice");

                        if (merchantName != null) {
                            // Count packages
                            merchantPackageCount.put(merchantName,
                                    merchantPackageCount.getOrDefault(merchantName, 0) + 1);

                            // Sum revenue
                            if (codPrice != null) {
                                merchantRevenue.put(merchantName,
                                        merchantRevenue.getOrDefault(merchantName, 0.0) + codPrice);
                            }
                        }
                    }

                    calculatePerformanceMetrics(merchantPackageCount, merchantRevenue);
                });
    }

    private void loadRevenueDistribution() {
        // Sample data for revenue distribution
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(35f, "FashionHub"));
        entries.add(new PieEntry(25f, "ElectroWorld"));
        entries.add(new PieEntry(15f, "HomeEssentials"));
        entries.add(new PieEntry(10f, "BookStore"));
        entries.add(new PieEntry(15f, "Others"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#4ECDC4"),
                Color.parseColor("#45B7D1"),
                Color.parseColor("#96CEB4"),
                Color.parseColor("#FFEAA7")
        });
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        merchantRevenueChart.setData(data);
        merchantRevenueChart.invalidate();
    }

    private void loadGrowthData() {
        // Sample growth data
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 45));
        entries.add(new Entry(1, 52));
        entries.add(new Entry(2, 48));
        entries.add(new Entry(3, 61));
        entries.add(new Entry(4, 75));
        entries.add(new Entry(5, 82));

        LineDataSet dataSet = new LineDataSet(entries, "Merchant Growth");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);

        LineData lineData = new LineData(dataSet);
        merchantGrowthChart.setData(lineData);
        merchantGrowthChart.invalidate();
    }

    private void calculatePerformanceMetrics(Map<String, Integer> packageCount, Map<String, Double> revenue) {
        // Calculate average packages per merchant
        if (!packageCount.isEmpty()) {
            double totalPackages = packageCount.values().stream().mapToInt(Integer::intValue).sum();
            double avgPackages = totalPackages / packageCount.size();
            avgPackagesPerMerchantText.setText(String.format(Locale.getDefault(), "%.0f", avgPackages));
        }

        // Calculate top merchant share
        if (!revenue.isEmpty()) {
            double totalRevenue = revenue.values().stream().mapToDouble(Double::doubleValue).sum();
            double topRevenue = revenue.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double topShare = (topRevenue / totalRevenue) * 100;
            topMerchantShareText.setText(String.format(Locale.getDefault(), "%.0f%%", topShare));
        }
    }
}
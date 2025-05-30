package com.softcraft.a1logistics;
// TrackingActivity.java

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackingActivity extends BaseActivity {
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private LinearLayout searchContainer;
    private PackageAdapter adapter;
    private final List<Map<String, Object>> packages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        setupToolbar(findViewById(R.id.toolbar), "Track Package");
        initializeViews();
        setupSearchButton();

        // Check if launched with a tracking number
        String trackingNumber = getIntent().getStringExtra("tracking_number");
        if (trackingNumber != null && !trackingNumber.isEmpty()) {
            searchEditText.setText(trackingNumber);
            searchPackages(trackingNumber);
        }
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchContainer = findViewById(R.id.searchContainer);

        adapter = new PackageAdapter(packages, position -> {
            Map<String, Object> packageData = packages.get(position);
            PackageDetailActivity.start(this, packageData, true);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Initially hide the recyclerView and show empty message
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText("Enter Order ID or Phone Number and click Search");
    }

    private void setupSearchButton() {
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter Order ID or Phone Number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Clear previous results
            packages.clear();
            adapter.notifyDataSetChanged();

            searchPackages(query);
        });
    }

    private void searchPackages(String query) {
        showLoading(true);
        packages.clear();
        adapter.notifyDataSetChanged();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First search by exact order ID match
        db.collection("PickupRequests")
                .whereEqualTo("orderId", query.trim())
                .limit(1) // We only need one match for exact order ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Exact match found
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> packageData = document.getData();
                                packageData.put("id", document.getId());
                                packages.add(packageData);
                            }
                            showResults();
                        } else {
                            // If no exact match, try partial match or phone number
                            searchPartialOrPhoneNumber(db, query);
                        }
                    } else {
                        showError();
                    }
                });
    }

    private void searchPartialOrPhoneNumber(FirebaseFirestore db, String query) {
        // Try partial match on order ID
        db.collection("PickupRequests")
                .orderBy("orderId")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .addOnCompleteListener(partialTask -> {
                    if (partialTask.isSuccessful() && !partialTask.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : partialTask.getResult()) {
                            Map<String, Object> packageData = document.getData();
                            packageData.put("id", document.getId());
                            packages.add(packageData);
                        }
                        showResults();
                    } else {
                        // If no partial match, try phone number
                        searchByPhoneNumber(db, query);
                    }
                });
    }

    private void searchByPhoneNumber(FirebaseFirestore db, String phoneNumber) {
        // Remove any non-digit characters from phone number
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");

        db.collection("PickupRequests")
                .whereEqualTo("customerNumber", cleanPhone)
                .get()
                .addOnCompleteListener(phoneTask -> {
                    showLoading(false);
                    if (phoneTask.isSuccessful()) {
                        for (QueryDocumentSnapshot document : phoneTask.getResult()) {
                            Map<String, Object> packageData = document.getData();
                            packageData.put("id", document.getId());
                            packages.add(packageData);
                        }
                        showResults();
                    } else {
                        showError();
                    }
                });
    }
    private void showResults() {
        showLoading(false);
        adapter.notifyDataSetChanged();

        if (packages.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No packages found for your search");
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!loading);
    }

    private void showError() {
        showLoading(false);
        Toast.makeText(this, "Error searching packages", Toast.LENGTH_SHORT).show();
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText("Error searching. Please try again.");
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter_menu, menu);
        MenuItem filterItem = menu.findItem(R.id.action_filter);
        if (filterItem != null) filterItem.setVisible(false);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) searchItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
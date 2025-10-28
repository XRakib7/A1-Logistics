package com.softcraft.a1logistics;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.airbnb.lottie.LottieAnimationView;

public class AllPackagesActivity extends BaseActivity {

    private static final String TAG = "AllPackagesActivity";
    private static final int STORAGE_PERMISSION_CODE = 100;

    // Enhanced Pagination variables
    private static final int PAGE_SIZE = 20; // Number of items per page
    private int currentPage = 1;
    private int totalPackagesCount = 0;
    private List<Map<String, Object>> allFilteredPackages = new ArrayList<>();
    private Button prevPageButton, nextPageButton;
    private TextView pageInfoTextView;
    private LinearLayout paginationLayout;
    private TextView emptyView;

    private RecyclerView recyclerView;
    private LottieAnimationView progressLoader;
    private PackageAdapter adapter;
    private List<Map<String, Object>> packagesList = new ArrayList<>();
    private FirebaseFirestore db;
    private Toolbar toolbar;

    // Filter variables
    private List<String> selectedStatuses = new ArrayList<>();
    private Date fromDate = null;
    private Date toDate = null;
    private String packageType = "all";

    private SearchView searchView;
    private String currentSearchQuery = "";
    private static final String SORT_CREATED_NEWEST = "created_desc";
    private static final String SORT_CREATED_OLDEST = "created_asc";
    private static final String SORT_UPDATED_NEWEST = "updated_desc";
    private static final String SORT_UPDATED_OLDEST = "updated_asc";
    private String currentSort = SORT_UPDATED_NEWEST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_packages);

        // Initialize with empty lists
        packagesList = new ArrayList<>();
        allFilteredPackages = new ArrayList<>();

        // Get package type from intent (if coming from AdminDashboard)
        if (getIntent().hasExtra("packageType")) {
            packageType = getIntent().getStringExtra("packageType");
        }

        db = FirebaseFirestore.getInstance();

        // Initialize ALL UI components first
        initializeViews();

        // Set appropriate title based on package type
        String title = "All Packages";
        switch (packageType) {
            case "active":
                title = "Active Packages";
                break;
            case "delivered":
                title = "Delivered Packages";
                break;
            case "returned":
                title = "Returned Packages";
                break;
        }
        setupToolbar(toolbar, title);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PackageAdapter(packagesList, this::onPackageClicked);
        recyclerView.setAdapter(adapter);

        // Set default status filters based on package type
        setDefaultStatusFilters();

        // Load packages
        loadPackages();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        progressLoader = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Initialize pagination views
        paginationLayout = findViewById(R.id.paginationLayout);
        prevPageButton = findViewById(R.id.prevPageButton);
        nextPageButton = findViewById(R.id.nextPageButton);
        pageInfoTextView = findViewById(R.id.pageInfoTextView);

        // Set initial visibility
        progressLoader.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        paginationLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        // Set up pagination button listeners
        prevPageButton.setOnClickListener(v -> goToPreviousPage());
        nextPageButton.setOnClickListener(v -> goToNextPage());
    }

    private void setDefaultStatusFilters() {
        switch (packageType) {
            case "active":
                selectedStatuses = Arrays.asList(
                        "Pickup Pending",
                        "Picked Up",
                        "In Transit",
                        "Out For Delivery",
                        "Hold"
                );
                break;
            case "delivered":
                selectedStatuses = Arrays.asList(
                        "Delivered",
                        "Paid To The Merchant"
                );
                break;
            case "returned":
                selectedStatuses = Arrays.asList(
                        "Returned By The Customer",
                        "Returned To The Merchant"
                );
                break;
            case "all":
            default:
                selectedStatuses = new ArrayList<>();
                break;
        }
    }

    private void loadPackages() {
        progressLoader.setVisibility(View.VISIBLE);
        progressLoader.playAnimation();
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        paginationLayout.setVisibility(View.GONE);

        Map<String, String> currentUser = getCurrentUser();
        boolean isAdmin = UserUtils.isAdmin(currentUser);
        String merchantUid = currentUser.get("uid");

        Query query = db.collection("PickupRequests");

        // Apply merchant filter by ID (not name)
        if (!isAdmin) {
            query = query.whereEqualTo("merchantId", merchantUid);
        }

        // Apply status filter
        if (!selectedStatuses.isEmpty()) {
            query = query.whereIn("status", selectedStatuses);
        }

        // Apply sorting
        try {
            switch (currentSort) {
                case SORT_CREATED_NEWEST:
                    query = query.orderBy("createdDate", Query.Direction.DESCENDING);
                    break;
                case SORT_CREATED_OLDEST:
                    query = query.orderBy("createdDate", Query.Direction.ASCENDING);
                    break;
                case SORT_UPDATED_NEWEST:
                    query = query.orderBy("lastUpdate", Query.Direction.DESCENDING);
                    break;
                case SORT_UPDATED_OLDEST:
                    query = query.orderBy("lastUpdate", Query.Direction.ASCENDING);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Sorting error, using default", e);
            query = query.orderBy("lastUpdate", Query.Direction.DESCENDING);
        }

        query.get().addOnCompleteListener(task -> {
            progressLoader.setVisibility(View.GONE);
            progressLoader.pauseAnimation();

            if (task.isSuccessful()) {
                allFilteredPackages.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, Object> packageData = document.getData();
                    packageData.put("documentId", document.getId());

                    Timestamp createdTimestamp = (Timestamp) packageData.get("createdDate");
                    if (createdTimestamp != null) {
                        Date createdDate = createdTimestamp.toDate();
                        boolean includeRecord = fromDate == null || !createdDate.before(getStartOfDay(fromDate));

                        if (toDate != null && createdDate.after(getEndOfDay(toDate))) {
                            includeRecord = false;
                        }

                        // Apply search filter if there's a query
                        if (includeRecord && (currentSearchQuery.isEmpty() || matchesSearchQuery(packageData, currentSearchQuery))) {
                            allFilteredPackages.add(packageData);
                        }
                    }
                }

                totalPackagesCount = allFilteredPackages.size();

                // Reset to first page when filters change
                currentPage = 1;

                // Load current page data
                loadCurrentPageData();

                // Update UI based on results
                if (packagesList.isEmpty()) {
                    showEmptyState();
                } else {
                    showContentState();
                }

                // Update pagination UI
                updatePaginationUI();

            } else {
                handleLoadError(task.getException());
                showEmptyState();
            }
        });
    }

    private void loadCurrentPageData() {
        packagesList.clear();

        if (allFilteredPackages.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalPackagesCount);

        for (int i = startIndex; i < endIndex; i++) {
            packagesList.add(allFilteredPackages.get(i));
        }

        adapter.notifyDataSetChanged();

        // Show toast for page change (except first load)
        if (currentPage > 1) {
            Toast.makeText(this, "Page " + currentPage + " loaded", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 📈 Enhanced Pagination UI with Smart Visibility
     */
    private void updatePaginationUI() {
        int totalPages = (int) Math.ceil((double) totalPackagesCount / PAGE_SIZE);

        // Smart pagination visibility
        if (totalPackagesCount <= PAGE_SIZE && currentPage == 1) {
            // Hide pagination for single page with <= PAGE_SIZE items
            paginationLayout.setVisibility(View.GONE);
        } else {
            paginationLayout.setVisibility(View.VISIBLE);
        }

        // Update button states
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);

        // Update page info with enhanced formatting
        updatePageInfoText(totalPages);
    }

    private void updatePageInfoText(int totalPages) {
        if (totalPackagesCount == 0) {
            pageInfoTextView.setText("No packages");
            return;
        }

        int startItem = ((currentPage - 1) * PAGE_SIZE) + 1;
        int endItem = Math.min(currentPage * PAGE_SIZE, totalPackagesCount);

        String pageInfo;
        if (totalPages > 1) {
            // Multi-page format with line break
            pageInfo = String.format(Locale.getDefault(),
                    "Page %d/%d\n(%d-%d of %d)",
                    currentPage, totalPages, startItem, endItem, totalPackagesCount);
        } else {
            // Single page format
            if (totalPackagesCount == 1) {
                pageInfo = "Page 1/1\n(1 item)";
            } else {
                pageInfo = String.format(Locale.getDefault(),
                        "Page 1/1\n(%d items)", totalPackagesCount);
            }
        }

        pageInfoTextView.setText(pageInfo);
    }

    private void goToPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadCurrentPageData();
            updatePaginationUI();
            recyclerView.smoothScrollToPosition(0); // Scroll to top
        }
    }

    private void goToNextPage() {
        int totalPages = (int) Math.ceil((double) totalPackagesCount / PAGE_SIZE);
        if (currentPage < totalPages) {
            currentPage++;
            loadCurrentPageData();
            updatePaginationUI();
            recyclerView.smoothScrollToPosition(0); // Scroll to top
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        paginationLayout.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void showContentState() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private boolean matchesSearchQuery(Map<String, Object> packageData, String query) {
        if (query.isEmpty()) return true;

        String[] searchFields = {
                "orderId",
                "customerName",
                "customerNumber",
                "deliveryLocation",
                "codPrice",
                "status",
                "merchantName",
                "pickupLocation",
                "packageDetails"
        };

        for (String field : searchFields) {
            Object value = packageData.get(field);
            if (value != null) {
                String stringValue = value.toString().toLowerCase(Locale.getDefault());
                if (stringValue.contains(query)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleLoadError(Exception exception) {
        if (exception != null && exception.getMessage().contains("index")) {
            Toast.makeText(this,
                    "Database indexes are being prepared. Please try again in a few minutes.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    "Error loading packages: " + (exception != null ? exception.getMessage() : "Unknown error"),
                    Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, "Error loading packages", exception);
    }

    private boolean isAdmin() {
        Map<String, String> user = getCurrentUser();
        return UserUtils.isAdmin(user);
    }

    // Get start of the day (00:00:00.000)
    private Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    // Get end of the day (23:59:59.999)
    private Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private void showStatusUpdateDialog(String documentId, String currentStatus) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Package Status");

        // Get both forward and backward options
        List<String> statusOptions = getStatusOptions(currentStatus);

        builder.setItems(statusOptions.toArray(new String[0]), (dialog, which) -> {
            String selectedStatus = statusOptions.get(which);
            showRemarksDialog(documentId, currentStatus, selectedStatus);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private List<String> getStatusOptions(String currentStatus) {
        List<String> options = new ArrayList<>();
        options.addAll(getNextStatusOptions(currentStatus));

        switch (currentStatus) {
            case "Picked Up":
                options.add("« Pickup Pending");
                break;
            case "In Transit":
                options.add("« Picked Up");
                break;
            case "Out For Delivery":
                options.add("« In Transit");
                break;
            case "Hold":
                options.add("« Out For Delivery");
                break;
            case "Delivered":
            case "Returned By The Customer":
                options.add("« Out For Delivery");
                options.add("« Hold");
                break;
            case "Paid To The Merchant":
                options.add("« Delivered");
                break;
            case "Returned To The Merchant":
                options.add("« Returned By The Customer");
                break;
        }
        return options;
    }

    private List<String> getNextStatusOptions(String currentStatus) {
        switch (currentStatus) {
            case "Pickup Pending":
                return List.of("Picked Up");
            case "Picked Up":
                return List.of("In Transit");
            case "In Transit":
                return List.of("Out For Delivery");
            case "Out For Delivery":
                return Arrays.asList("Delivered", "Returned By The Customer", "Hold");
            case "Hold":
                return Arrays.asList("Out For Delivery", "Delivered", "Returned By The Customer");
            case "Delivered":
                return List.of("Paid To The Merchant");
            case "Returned By The Customer":
                return List.of("Returned To The Merchant");
            default:
                return new ArrayList<>();
        }
    }

    private void showHoldDialog(String documentId, String currentStatus, String newStatus) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Hold Reason");

        String[] holdReasons = {
                "Customer Not Available",
                "Incorrect Address",
                "Payment Issues",
                "Customer Requested Later Delivery",
                "Security Concerns",
                "Weather Conditions",
                "Vehicle Breakdown",
                "Custom Reason"
        };

        final String[] selectedReason = {holdReasons[0]};
        final EditText customInput = new EditText(this);
        customInput.setHint("Enter custom reason");
        customInput.setVisibility(View.GONE);

        customInput.setMinHeight(dpToPx(48));
        customInput.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));

        RadioGroup reasonGroup = new RadioGroup(this);
        for (int i = 0; i < holdReasons.length; i++) {
            RadioButton radio = new RadioButton(this);
            radio.setText(holdReasons[i]);
            radio.setId(i);

            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.topMargin = dpToPx(8);
            }
            radio.setLayoutParams(params);

            reasonGroup.addView(radio);
            if (i == 0) radio.setChecked(true);
        }

        reasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            selectedReason[0] = holdReasons[checkedId];
            boolean showCustom = checkedId == holdReasons.length - 1;
            customInput.setVisibility(showCustom ? View.VISIBLE : View.GONE);

            if (showCustom) {
                scrollView.postDelayed(() -> {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    customInput.requestFocus();
                }, 100);
            }
        });

        layout.addView(reasonGroup);

        TextView customLabel = new TextView(this);
        customLabel.setText("Custom Reason:");
        customLabel.setVisibility(View.GONE);
        customLabel.setTextSize(14);
        customLabel.setTextColor(getResources().getColor(android.R.color.black));
        customLabel.setPadding(0, dpToPx(16), 0, dpToPx(8));

        layout.addView(customLabel);
        layout.addView(customInput);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String remarks;
            if (selectedReason[0].equals("Custom Reason")) {
                remarks = customInput.getText().toString().trim();
                if (remarks.isEmpty()) remarks = "Custom Hold";
            } else {
                remarks = selectedReason[0];
            }

            String cleanStatus = newStatus.replace("? ", "").replace("<- ", "");
            updatePackageStatus(documentId, cleanStatus, remarks);
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showRemarksDialog(String documentId, String currentStatus, String newStatus) {
        if (newStatus.equals("Hold")) {
            showHoldDialog(documentId, currentStatus, newStatus);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Remarks (Optional)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter remarks if any");
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String remarks = input.getText().toString().trim();
            updatePackageStatus(documentId, newStatus.replace("« ", ""), remarks);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updatePackageStatus(String documentId, String newStatus, String remarks) {
        if (!isAdmin()) {
            Toast.makeText(this, "Only admin can update status", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = getCurrentUser().get("uid");

        db.collection("PickupRequests").document(documentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<Map<String, Object>> statusHistory = new ArrayList<>();
                            if (document.contains("statusHistory")) {
                                statusHistory = (List<Map<String, Object>>) document.get("statusHistory");
                            }

                            Map<String, Object> newStatusEntry = new HashMap<>();
                            newStatusEntry.put("status", newStatus);
                            newStatusEntry.put("updateTime", new Date());
                            newStatusEntry.put("updatedBy", currentUserId);
                            if (!remarks.isEmpty()) {
                                newStatusEntry.put("remarks", remarks);
                            }

                            statusHistory.add(newStatusEntry);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", newStatus);
                            updates.put("statusHistory", statusHistory);
                            updates.put("lastUpdate", FieldValue.serverTimestamp());

                            db.collection("PickupRequests").document(documentId)
                                    .update(updates)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                                            loadPackages();
                                        } else {
                                            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                });
    }

    private void onPackageClicked(int position) {
        Map<String, Object> selectedPackage = packagesList.get(position);
        if (isAdmin()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Admin Options");
            builder.setItems(new String[]{"View Details", "Update Status"}, (dialog, which) -> {
                if (which == 0) {
                    openPackageDetails(selectedPackage);
                } else {
                    String currentStatus = (String) selectedPackage.get("status");
                    showStatusUpdateDialog((String) selectedPackage.get("documentId"), currentStatus);
                }
            });
            builder.show();
        } else {
            openPackageDetails(selectedPackage);
        }
    }

    private void openPackageDetails(Map<String, Object> packageData) {
        Intent intent = new Intent(this, PackageDetailActivity.class);
        intent.putExtra("packageData", new HashMap<>(packageData));
        intent.putExtra("isAdmin", isAdmin());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search packages...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query.toLowerCase(Locale.getDefault());
                currentPage = 1; // Reset to first page when searching
                loadPackages();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText.toLowerCase(Locale.getDefault());
                currentPage = 1; // Reset to first page when searching
                loadPackages();
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (item.getItemId() == R.id.action_download) {
            if (packagesList == null || packagesList.isEmpty()) {
                Toast.makeText(this, "No packages to download", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Download button clicked, packages count: " + packagesList.size());
                checkAndRequestStoragePermission();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            DownloadUtils.downloadPackageList(this, packagesList, isAdmin());
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DownloadUtils.downloadPackageList(this, packagesList, isAdmin());
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RecyclerView statusRecyclerView = dialogView.findViewById(R.id.statusRecyclerView);
        TextInputEditText fromDateEditText = dialogView.findViewById(R.id.fromDateEditText);
        TextInputEditText toDateEditText = dialogView.findViewById(R.id.toDateEditText);
        Button applyFilterButton = dialogView.findViewById(R.id.applyFilterButton);
        Button clearFilterButton = dialogView.findViewById(R.id.clearFilterButton);
        RadioGroup sortRadioGroup = dialogView.findViewById(R.id.sortRadioGroup);

        // Set default sort selection
        switch (currentSort) {
            case SORT_CREATED_NEWEST:
                sortRadioGroup.check(R.id.sortCreatedNewest);
                break;
            case SORT_CREATED_OLDEST:
                sortRadioGroup.check(R.id.sortCreatedOldest);
                break;
            case SORT_UPDATED_NEWEST:
                sortRadioGroup.check(R.id.sortUpdatedNewest);
                break;
            case SORT_UPDATED_OLDEST:
                sortRadioGroup.check(R.id.sortUpdatedOldest);
                break;
        }

        // Determine which statuses to show based on package type
        List<String> allStatuses = getStatusesForPackageType();

        StatusFilterAdapter statusAdapter = new StatusFilterAdapter(allStatuses);
        statusRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        statusRecyclerView.setAdapter(statusAdapter);
        statusAdapter.setSelectedStatuses(selectedStatuses);

        fromDateEditText.setOnClickListener(v -> showDatePickerDialog(fromDateEditText));
        toDateEditText.setOnClickListener(v -> showDatePickerDialog(toDateEditText));

        applyFilterButton.setOnClickListener(v -> {
            selectedStatuses = statusAdapter.getSelectedStatuses();

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                fromDate = fromDateEditText.getText().toString().isEmpty() ?
                        null : sdf.parse(fromDateEditText.getText().toString());
                toDate = toDateEditText.getText().toString().isEmpty() ?
                        null : sdf.parse(toDateEditText.getText().toString());

                if (fromDate != null && toDate != null && fromDate.after(toDate)) {
                    Toast.makeText(this, "From date cannot be after To date", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateCurrentSort(sortRadioGroup);
                currentPage = 1; // Reset to first page when applying filters
                loadPackages();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            }
        });

        clearFilterButton.setOnClickListener(v -> {
            statusAdapter.selectAll(true);
            fromDateEditText.setText("");
            toDateEditText.setText("");
            sortRadioGroup.check(R.id.sortUpdatedNewest);
            currentSort = SORT_UPDATED_NEWEST;
            setDefaultStatusFilters();
            currentPage = 1; // Reset to first page when clearing filters
            loadPackages();
            dialog.dismiss();
        });

        dialog.show();
    }

    private List<String> getStatusesForPackageType() {
        switch (packageType) {
            case "active":
                return Arrays.asList(
                        "Pickup Pending", "Picked Up", "In Transit", "Out For Delivery", "Hold");
            case "delivered":
                return Arrays.asList("Delivered", "Paid To The Merchant");
            case "returned":
                return Arrays.asList("Returned By The Customer", "Returned To The Merchant");
            case "all":
            default:
                return Arrays.asList(
                        "Pickup Pending", "Picked Up", "In Transit", "Out For Delivery", "Hold",
                        "Delivered", "Paid To The Merchant",
                        "Returned By The Customer", "Returned To The Merchant");
        }
    }

    private void updateCurrentSort(RadioGroup sortRadioGroup) {
        int selectedId = sortRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.sortCreatedNewest) {
            currentSort = SORT_CREATED_NEWEST;
        } else if (selectedId == R.id.sortCreatedOldest) {
            currentSort = SORT_CREATED_OLDEST;
        } else if (selectedId == R.id.sortUpdatedNewest) {
            currentSort = SORT_UPDATED_NEWEST;
        } else if (selectedId == R.id.sortUpdatedOldest) {
            currentSort = SORT_UPDATED_OLDEST;
        }
    }


}
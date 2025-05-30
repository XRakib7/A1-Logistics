package com.softcraft.a1logistics;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class PackageDetailActivity extends BaseActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "PackageDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_detail);

        boolean isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        setupToolbar(findViewById(R.id.toolbar), "Package Details");
        findViewById(R.id.merchantInfoCard).setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        Map<String, Object> packageData = (Map<String, Object>) getIntent().getSerializableExtra("packageData");

        if (packageData != null) {
            initializeViews(packageData);

            if (packageData.containsKey("statusHistory")) {
                List<Map<String, Object>> history = (List<Map<String, Object>>) packageData.get("statusHistory");
                displayStatusHistory(history);
            }
        }
    }
    public static void start(AppCompatActivity activity, Map<String, Object> packageData, boolean isAdmin) {
        Intent intent = new Intent(activity, PackageDetailActivity.class);
        intent.putExtra("packageData", (Serializable) packageData);
        intent.putExtra("isAdmin", isAdmin);
        activity.startActivity(intent);
    }
    private void initializeViews(Map<String, Object> packageData) {
        TextView orderIdText = findViewById(R.id.orderIdTextView);
        TextView customerNameText = findViewById(R.id.customerNameTextView);
        TextView customerNumberText = findViewById(R.id.customerNumberTextView);
        TextView deliveryLocationText = findViewById(R.id.deliveryLocationTextView);
        TextView codPriceText = findViewById(R.id.codPriceTextView);
        TextView packageDetailsText = findViewById(R.id.packageDetailsText);
        TextView packageWeightText = findViewById(R.id.packageWeightText);
        TextView itemQuantityText = findViewById(R.id.itemQuantityText);
        TextView packageValueText = findViewById(R.id.packageValueText);
        TextView pickupLocationText = findViewById(R.id.pickupLocationText);
        TextView merchantIdText = findViewById(R.id.merchantIdText);
        TextView merchantNameText = findViewById(R.id.merchantNameText);
        TextView statusText = findViewById(R.id.statusTextView);
        TextView createdDateText = findViewById(R.id.createdDateText);
        TextView lastUpdateText = findViewById(R.id.lastUpdateText);


        setTextSafe(orderIdText, packageData.get("orderId"));
        setTextSafe(customerNameText, packageData.get("customerName"));
        setTextSafe(customerNumberText, packageData.get("customerNumber"));
        setTextSafe(deliveryLocationText, packageData.get("deliveryLocation"));
        setTextSafe(codPriceText, "BDT " + packageData.get("codPrice"));
        setTextSafe(packageDetailsText, packageData.get("packageDetails"));
        setTextSafe(packageWeightText, packageData.get("packageWeight") + " KG");
        setTextSafe(itemQuantityText, packageData.get("itemQuantity"));
        setTextSafe(packageValueText, "BDT " + packageData.get("packageValue"));
        setTextSafe(pickupLocationText, packageData.get("pickupLocation"));
        setTextSafe(merchantNameText, packageData.get("merchantName"));
        setTextSafe(merchantIdText, packageData.get("merchantId"));
        setTextSafe(statusText, packageData.get("status"));

        Object createdDateObj = packageData.get("createdDate");
        if (createdDateObj != null) {
            Date createdDate = convertToDate(createdDateObj);
            if (createdDate != null) {
                createdDateText.setText(formatDate(createdDate));
            }
        }
        Object lastUpdateObj = packageData.get("lastUpdate");
        if (lastUpdateObj != null) {
            Date lastUpdate = convertToDate(lastUpdateObj);
            if (lastUpdateObj != null) {
                lastUpdateText.setText(formatDate(lastUpdate));
            }
        }

        setSelectable(orderIdText, customerNameText, customerNumberText, deliveryLocationText,
                codPriceText, packageDetailsText, packageWeightText, pickupLocationText,
                merchantIdText, merchantNameText, statusText, createdDateText, lastUpdateText,
                itemQuantityText, packageValueText);
    }

    private void displayStatusHistory(List<Map<String, Object>> history) {
        RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Collections.sort(history, (o1, o2) -> {
            Timestamp t1 = (Timestamp) o1.get("updateTime");
            Timestamp t2 = (Timestamp) o2.get("updateTime");
            return t2.compareTo(t1);
        });

        StatusHistoryAdapter adapter = new StatusHistoryAdapter(history,
                uid -> UserUtils.showUserDetails(this, uid)
        );
        historyRecyclerView.setAdapter(adapter);
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
        if (item.getItemId() == R.id.action_download) {
            downloadPackage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadPackage() {
        Map<String, Object> packageData = (Map<String, Object>) getIntent().getSerializableExtra("packageData");
        if (packageData == null) {
            Toast.makeText(this, "No package data available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            boolean isAdmin = getIntent().getBooleanExtra("isAdmin", false);
            DownloadUtils.downloadPackageDetails(this, packageData, isAdmin);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadPackage();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Date convertToDate(Object dateObj) {
        if (dateObj instanceof Timestamp) {
            return ((Timestamp) dateObj).toDate();
        } else if (dateObj instanceof Date) {
            return (Date) dateObj;
        }
        return null;
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date);
    }

    private void setTextSafe(TextView textView, Object value) {
        if (textView != null) {
            textView.setText(value != null ? value.toString() : "N/A");
        }
    }

    private void setSelectable(TextView... textViews) {
        for (TextView tv : textViews) {
            if (tv != null) {
                tv.setTextIsSelectable(true);
            }
        }
    }
}
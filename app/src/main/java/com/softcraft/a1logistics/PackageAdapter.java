package com.softcraft.a1logistics;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {
    private static final String TAG = "PackageAdapter";
    private final List<Map<String, Object>> packages;
    private final OnPackageClickListener listener;

    public interface OnPackageClickListener {
        void onPackageClick(int position);
    }

    public PackageAdapter(List<Map<String, Object>> packages, OnPackageClickListener listener) {
        this.packages = packages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package, parent, false);
        return new PackageViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        Map<String, Object> packageData = packages.get(position);

        try {
            String orderId = getSafeString(packageData, "orderId");
            String customerName = getSafeString(packageData, "customerName");
            String customerNumber = getSafeString(packageData, "customerNumber");
            String deliveryLocation = getSafeString(packageData, "deliveryLocation");
            String codPrice = getSafeString(packageData, "codPrice");
            String status = getSafeString(packageData, "status");

            // Highlight search results if there's a query
            if (listener instanceof AllPackagesActivity) {
                String query = ((AllPackagesActivity) listener).getCurrentSearchQuery();
                highlightText(holder.orderIdText, orderId, query);
                highlightText(holder.customerNameText, customerName, query);
                highlightText(holder.customerNumberText, customerNumber, query);
                highlightText(holder.deliveryLocationText, deliveryLocation, query);
                highlightText(holder.codPriceText, codPrice, query);
                highlightText(holder.statusText, status, query);
            } else {
                holder.orderIdText.setText(orderId);
                holder.customerNameText.setText(customerName);
                holder.customerNumberText.setText(customerNumber);
                holder.deliveryLocationText.setText(deliveryLocation);
                holder.codPriceText.setText(codPrice);
                holder.statusText.setText(status);
            }

            // Format and display dates
            Timestamp createdDate = (Timestamp) packageData.get("createdDate");
            Timestamp lastUpdate = (Timestamp) packageData.get("lastUpdate");

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

            if (createdDate != null) {
                holder.createdDateText.setText("Created: " + sdf.format(createdDate.toDate()));
                holder.createdDateText.setVisibility(View.VISIBLE);
            } else {
                holder.createdDateText.setVisibility(View.GONE);
            }

            if (lastUpdate != null) {
                holder.lastUpdateText.setText("Updated: " + sdf.format(lastUpdate.toDate()));
                holder.lastUpdateText.setVisibility(View.VISIBLE);
            } else {
                holder.lastUpdateText.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding package data", e);
        }
    }
    public void highlightText(TextView textView, String text, String query) {
        if (text == null || query == null || query.isEmpty()) {
            textView.setText(text);
            return;
        }

        SpannableString spannable = new SpannableString(text);
        String lowerText = text.toLowerCase(Locale.getDefault());
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        int startPos = lowerText.indexOf(lowerQuery);

        if (startPos != -1) {
            int endPos = startPos + query.length();
            spannable.setSpan(
                    new ForegroundColorSpan(Color.YELLOW),
                    startPos,
                    endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannable.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    startPos,
                    endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        textView.setText(spannable);
    }
    private String getSafeString(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return "N/A";
        }
        return map.get(key).toString();
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    static class PackageViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText, customerNameText, customerNumberText,
                deliveryLocationText, codPriceText, statusText,
                createdDateText, lastUpdateText;

        public PackageViewHolder(@NonNull View itemView, OnPackageClickListener listener) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.orderIdText);
            customerNameText = itemView.findViewById(R.id.customerNameText);
            customerNumberText = itemView.findViewById(R.id.customerNumberText);
            deliveryLocationText = itemView.findViewById(R.id.deliveryLocationText);
            codPriceText = itemView.findViewById(R.id.codPriceText);
            statusText = itemView.findViewById(R.id.statusText);
            createdDateText = itemView.findViewById(R.id.createdDateText);
            lastUpdateText = itemView.findViewById(R.id.lastUpdateText);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onPackageClick(getAdapterPosition());
                }
            });
        }
    }

}
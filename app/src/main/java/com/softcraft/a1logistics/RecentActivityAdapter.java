package com.softcraft.a1logistics;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final List<Map<String, Object>> recentActivities;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public RecentActivityAdapter(List<Map<String, Object>> recentActivities) {
        this.recentActivities = recentActivities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> activity = recentActivities.get(position);

        holder.orderIdText.setText("#" + activity.get("orderId"));
        holder.customerNameText.setText(String.valueOf(activity.get("customerName")));
        holder.statusText.setText(String.valueOf(activity.get("status")));
        holder.locationText.setText(String.valueOf(activity.get("deliveryLocation")));

        // Set status color
        String status = String.valueOf(activity.get("status"));
        int color = getStatusColor(status);
        holder.statusText.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return recentActivities.size();
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Delivered":
                return Color.parseColor("#4CAF50");
            case "Pickup Pending":
                return Color.parseColor("#FF9800");
            case "Returned":
                return Color.parseColor("#F44336");
            default:
                return Color.parseColor("#2196F3");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText, customerNameText, statusText, locationText;

        ViewHolder(View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.orderIdText);
            customerNameText = itemView.findViewById(R.id.customerNameText);
            statusText = itemView.findViewById(R.id.statusText);
            locationText = itemView.findViewById(R.id.locationText);
        }
    }
}
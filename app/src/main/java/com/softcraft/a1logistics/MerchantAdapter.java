package com.softcraft.a1logistics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class MerchantAdapter extends RecyclerView.Adapter<MerchantAdapter.MerchantViewHolder> {
    private final List<Map<String, Object>> merchants;

    public MerchantAdapter(List<Map<String, Object>> merchants) {
        this.merchants = merchants;
    }

    @NonNull
    @Override
    public MerchantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_merchant, parent, false);
        return new MerchantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MerchantViewHolder holder, int position) {
        Map<String, Object> merchant = merchants.get(position);
        holder.businessNameText.setText(getSafeString(merchant, "businessName"));
        holder.phoneText.setText(getSafeString(merchant, "phone"));
        holder.emailText.setText(getSafeString(merchant, "email"));
        holder.pickupLocationText.setText(getSafeString(merchant, "pickupLocation"));
    }

    private String getSafeString(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : "N/A";
    }

    @Override
    public int getItemCount() {
        return merchants.size();
    }

    static class MerchantViewHolder extends RecyclerView.ViewHolder {
        TextView businessNameText, phoneText, emailText, pickupLocationText;

        public MerchantViewHolder(@NonNull View itemView) {
            super(itemView);
            businessNameText = itemView.findViewById(R.id.businessNameText);
            phoneText = itemView.findViewById(R.id.phoneText);
            emailText = itemView.findViewById(R.id.emailText);
            pickupLocationText = itemView.findViewById(R.id.pickupLocationText);
        }
    }
}
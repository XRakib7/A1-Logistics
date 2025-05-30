package com.softcraft.a1logistics;

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

public class StatusHistoryAdapter extends RecyclerView.Adapter<StatusHistoryAdapter.ViewHolder> {
    private final List<Map<String, Object>> history;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(String uid);
    }

    public StatusHistoryAdapter(List<Map<String, Object>> history, OnUserClickListener listener) {
        this.history = history;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_status_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> entry = history.get(position);

        // Format date
        Timestamp timestamp = (Timestamp) entry.get("updateTime");
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(timestamp.toDate());

        holder.statusText.setText(entry.get("status").toString());
        holder.dateText.setText(date);

        // Make UID clickable
        holder.updatedByText.setText(entry.get("updatedBy").toString());
        holder.updatedByText.setOnClickListener(v ->
                listener.onUserClick(entry.get("updatedBy").toString())
        );

        // Handle remarks
        if (entry.containsKey("remarks")) {
            String remarks = entry.get("remarks").toString();
            if (!remarks.isEmpty()) {
                holder.remarksText.setText(remarks);
                holder.remarksText.setVisibility(View.VISIBLE);
            } else {
                holder.remarksText.setVisibility(View.GONE);
            }
        } else {
            holder.remarksText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView statusText, dateText, updatedByText, remarksText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusText = itemView.findViewById(R.id.statusText);
            dateText = itemView.findViewById(R.id.dateText);
            updatedByText = itemView.findViewById(R.id.updatedByText);
            remarksText = itemView.findViewById(R.id.remarksText);
        }
    }
}
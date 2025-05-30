package com.softcraft.a1logistics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusFilterAdapter extends RecyclerView.Adapter<StatusFilterAdapter.StatusViewHolder> {
    private final List<String> statusList;
    private final Map<String, Boolean> selectedStatusMap;

    public StatusFilterAdapter(List<String> statusList) {
        this.statusList = statusList;
        this.selectedStatusMap = new HashMap<>();
        for (String status : statusList) {
            selectedStatusMap.put(status, true); // All selected by default
        }
    }

    public void setSelectedStatuses(List<String> selectedStatuses) {
        // First reset all to false
        for (String status : statusList) {
            selectedStatusMap.put(status, false);
        }
        // Then set the provided ones to true
        for (String status : selectedStatuses) {
            if (selectedStatusMap.containsKey(status)) {
                selectedStatusMap.put(status, true);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_status_filter, parent, false);
        return new StatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        String status = statusList.get(position);
        holder.statusCheckBox.setText(status);
        holder.statusCheckBox.setChecked(selectedStatusMap.get(status));

        holder.statusCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedStatusMap.put(status, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    public List<String> getSelectedStatuses() {
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedStatusMap.entrySet()) {
            if (entry.getValue()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    public void selectAll(boolean selectAll) {
        for (String status : statusList) {
            selectedStatusMap.put(status, selectAll);
        }
        notifyDataSetChanged();
    }

    static class StatusViewHolder extends RecyclerView.ViewHolder {
        CheckBox statusCheckBox;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            statusCheckBox = itemView.findViewById(R.id.statusCheckBox);
        }
    }
}
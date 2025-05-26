package com.example.pbdv_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertAdapter extends FirestoreRecyclerAdapter<Alert, AlertAdapter.AlertHolder> {
    private final ViewAlertsActivity context;
    private OnAlertClickListener listener;
    private String targetAlertId = null;
    private String searchQuery = "";
    private final List<Alert> filteredAlerts = new ArrayList<>();
    private final List<DocumentSnapshot> filteredSnapshots = new ArrayList<>();

    public AlertAdapter(@NonNull FirestoreRecyclerOptions<Alert> options, ViewAlertsActivity context) {
        super(options);
        this.context = context;
        updateFilteredList();
    }

    public void setTargetAlertId(String alertId) {
        this.targetAlertId = alertId;
        notifyDataSetChanged();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase(Locale.getDefault());
        updateFilteredList();
    }

    private void updateFilteredList() {
        filteredAlerts.clear();
        filteredSnapshots.clear();

        for (int i = 0; i < getSnapshots().size(); i++) {
            Alert alert = getItem(i);
            DocumentSnapshot snapshot = getSnapshots().getSnapshot(i);
            if (shouldShowItem(alert)) {
                filteredAlerts.add(alert);
                filteredSnapshots.add(snapshot);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected void onBindViewHolder(@NonNull AlertHolder holder, int position, @NonNull Alert model) {
        Alert alert = filteredAlerts.get(position);

        holder.titleTextView.setText(alert.getTitle());
        holder.messageTextView.setText(alert.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        holder.timeTextView.setText(sdf.format(new Date(alert.getTimestampLong())));

        DocumentSnapshot snapshot = filteredSnapshots.get(position);
        if (!alert.isRead()) {
            snapshot.getReference().update("read", true);
        }

        // Highlight target alert if set
        if (targetAlertId != null && targetAlertId.equals(alert.getDocumentId())) {
            holder.itemView.setBackgroundResource(R.color.highlight_color);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
    }

    public boolean shouldShowItem(Alert alert) {
        if (alert == null) return false;
        if (searchQuery == null || searchQuery.isEmpty()) return true;

        return (alert.getTitle() != null && alert.getTitle().toLowerCase(Locale.getDefault()).contains(searchQuery)) ||
                (alert.getMessage() != null && alert.getMessage().toLowerCase(Locale.getDefault()).contains(searchQuery)) ||
                (alert.getType() != null && alert.getType().toLowerCase(Locale.getDefault()).contains(searchQuery));
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        updateFilteredList();
    }

    @NonNull
    @Override
    public AlertHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertHolder(view);
    }

    @Override
    public int getItemCount() {
        return filteredAlerts.size();
    }

    class AlertHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView messageTextView;
        TextView timeTextView;

        public AlertHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.alertTitle);
            messageTextView = itemView.findViewById(R.id.alertMessage);
            timeTextView = itemView.findViewById(R.id.alertTime);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAlertClick(filteredSnapshots.get(position), position);
                }
            });
        }
    }

    public interface OnAlertClickListener {
        void onAlertClick(DocumentSnapshot documentSnapshot, int position);
    }

    public void setOnAlertClickListener(OnAlertClickListener listener) {
        this.listener = listener;
    }

    public int getPositionForAlertId(String alertId) {
        if (alertId == null) return -1;

        for (int i = 0; i < filteredAlerts.size(); i++) {
            Alert alert = filteredAlerts.get(i);
            if (alert != null && alertId.equals(alert.getDocumentId())) {
                return i;
            }
        }
        return -1;
    }
}
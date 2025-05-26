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

public class ResolvedReportAdapter extends FirestoreRecyclerAdapter<Incident, ResolvedReportAdapter.ReportHolder> {
    private OnDataChangedListener onDataChangedListener;
    private String searchQuery = "";
    private final List<Incident> filteredIncidents = new ArrayList<>();
    private final List<DocumentSnapshot> filteredSnapshots = new ArrayList<>();

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public ResolvedReportAdapter(@NonNull FirestoreRecyclerOptions<Incident> options) {
        super(options);
        updateFilteredList();
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.onDataChangedListener = listener;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase(Locale.getDefault()).trim();
        updateFilteredList();
    }

    private void updateFilteredList() {
        filteredIncidents.clear();
        filteredSnapshots.clear();

        if (getSnapshots().isEmpty()) {
            notifyDataSetChanged();
            if (onDataChangedListener != null) {
                onDataChangedListener.onDataChanged();
            }
            return;
        }

        for (int i = 0; i < getSnapshots().size(); i++) {
            try {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(i);
                Incident incident = getItem(i);

                if (incident != null && matchesSearchQuery(incident)) {
                    filteredIncidents.add(incident);
                    filteredSnapshots.add(snapshot);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        notifyDataSetChanged();

        if (onDataChangedListener != null) {
            onDataChangedListener.onDataChanged();
        }
    }

    private boolean matchesSearchQuery(Incident incident) {
        if (searchQuery.isEmpty()) return true;

        String query = searchQuery.toLowerCase(Locale.getDefault());

        if (incident.getType() != null && incident.getType().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (incident.getDescription() != null && incident.getDescription().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (incident.getLocationText() != null && incident.getLocationText().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (!incident.isAnonymous() && incident.getUserName() != null && incident.getUserName().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (!incident.isAnonymous() && incident.getUserEmail() != null && incident.getUserEmail().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (!incident.isAnonymous() && incident.getUserPhone() != null && incident.getUserPhone().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (incident.getResolvedBy() != null && incident.getResolvedBy().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }

        return false;
    }

    public boolean shouldShowItem(Incident item) {
        return matchesSearchQuery(item);
    }

    @Override
    protected void onBindViewHolder(@NonNull ReportHolder holder, int position, @NonNull Incident model) {
        if (filteredIncidents.isEmpty() || position >= filteredIncidents.size()) {
            return;
        }

        Incident incident = filteredIncidents.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        String time = sdf.format(new Date(incident.getTimestamp()));
        String resolvedTime = sdf.format(new Date(incident.getResolvedTimestamp()));

        holder.textViewType.setText(incident.getType());
        holder.textViewTime.setText("Reported: " + time);
        holder.textViewResolvedTime.setText("Resolved: " + resolvedTime);
        holder.textViewDescription.setText(incident.getDescription());
        holder.textViewLocation.setText("Location: " + incident.getLocationText());

        if (incident.isAnonymous()) {
            holder.textViewReporter.setText("Anonymous Report");
            holder.textViewContact.setVisibility(View.GONE);
        } else {
            holder.textViewReporter.setText(incident.getUserName() != null && !incident.getUserName().isEmpty() ?
                    "Reported by: " + incident.getUserName() : "Unknown Reporter");

            StringBuilder contactInfo = new StringBuilder();
            if (incident.getUserEmail() != null && !incident.getUserEmail().isEmpty()) {
                contactInfo.append("Email: ").append(incident.getUserEmail());
            }
            if (incident.getUserPhone() != null && !incident.getUserPhone().isEmpty()) {
                if (contactInfo.length() > 0) contactInfo.append("\n");
                contactInfo.append("Phone: ").append(incident.getUserPhone());
            }

            if (contactInfo.length() > 0) {
                holder.textViewContact.setText(contactInfo.toString());
                holder.textViewContact.setVisibility(View.VISIBLE);
            } else {
                holder.textViewContact.setVisibility(View.GONE);
            }
        }

        if (incident.getResolvedBy() != null && !incident.getResolvedBy().isEmpty()) {
            holder.textViewResolvedBy.setText("Resolved by: " + incident.getResolvedBy());
            holder.textViewResolvedBy.setVisibility(View.VISIBLE);
        } else {
            holder.textViewResolvedBy.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        updateFilteredList();
    }

    @NonNull
    @Override
    public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resolved_report, parent, false);
        return new ReportHolder(view);
    }

    @Override
    public int getItemCount() {
        return filteredIncidents.size();
    }

    static class ReportHolder extends RecyclerView.ViewHolder {
        TextView textViewType;
        TextView textViewTime;
        TextView textViewResolvedTime;
        TextView textViewDescription;
        TextView textViewLocation;
        TextView textViewReporter;
        TextView textViewContact;
        TextView textViewResolvedBy;

        ReportHolder(View itemView) {
            super(itemView);
            textViewType = itemView.findViewById(R.id.reportType);
            textViewTime = itemView.findViewById(R.id.reportTime);
            textViewResolvedTime = itemView.findViewById(R.id.reportResolvedTime);
            textViewDescription = itemView.findViewById(R.id.reportDescription);
            textViewLocation = itemView.findViewById(R.id.reportLocation);
            textViewReporter = itemView.findViewById(R.id.reportReporter);
            textViewContact = itemView.findViewById(R.id.reportContact);
            textViewResolvedBy = itemView.findViewById(R.id.reportResolvedBy);
        }
    }
}
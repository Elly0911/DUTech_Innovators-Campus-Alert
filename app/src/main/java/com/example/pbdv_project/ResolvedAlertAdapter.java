package com.example.pbdv_project;

import android.graphics.Color;
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
import java.util.Map;

public class ResolvedAlertAdapter extends FirestoreRecyclerAdapter<PanicAlert, ResolvedAlertAdapter.AlertHolder> {
    private OnDataChangedListener onDataChangedListener;
    private String searchQuery = "";
    private final List<PanicAlert> filteredAlerts = new ArrayList<>();
    private final List<DocumentSnapshot> filteredSnapshots = new ArrayList<>();

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public ResolvedAlertAdapter(@NonNull FirestoreRecyclerOptions<PanicAlert> options) {
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
        filteredAlerts.clear();
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
                PanicAlert alert = getItem(i);  // Use getItem instead of snapshot.toObject

                if (alert != null && matchesSearchQuery(alert)) {
                    filteredAlerts.add(alert);
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

    private boolean matchesSearchQuery(PanicAlert alert) {
        if (searchQuery.isEmpty()) return true;

        String query = searchQuery.toLowerCase(Locale.getDefault());

        // Check all fields that could be searched
        if (alert.getUserName() != null && alert.getUserName().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (alert.getCategory() != null && alert.getCategory().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (alert.getUserPhone() != null && alert.getUserPhone().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (alert.getAddress() != null && alert.getAddress().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (String.format(Locale.getDefault(), "%.6f, %.6f",
                alert.getLatitude(), alert.getLongitude()).toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (alert.getEmergencyContactsList() != null) {
            for (Map<String, String> contact : alert.getEmergencyContactsList()) {
                if ((contact.get("name") != null && contact.get("name").toLowerCase(Locale.getDefault()).contains(query))) {
                    return true;
                }
                if (contact.get("phone") != null && contact.get("phone").toLowerCase(Locale.getDefault()).contains(query)) {
                    return true;
                }
            }
        }
        if (alert.getResolvedBy() != null && alert.getResolvedBy().toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }

        return false;
    }

    public boolean shouldShowItem(PanicAlert item) {
        return matchesSearchQuery(item);
    }

    @Override
    protected void onBindViewHolder(@NonNull AlertHolder holder, int position, @NonNull PanicAlert model) {
        if (filteredAlerts.isEmpty() || position >= filteredAlerts.size()) {
            return;
        }

        PanicAlert alert = filteredAlerts.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        String time = sdf.format(new Date(alert.getTimestamp()));
        String resolvedTime = sdf.format(new Date(alert.getResolvedTimestamp()));

        holder.textViewName.setText(alert.getUserName());
        holder.textViewTime.setText(String.format("Alert Time: %s", time));
        holder.textViewResolvedTime.setText(String.format("Resolved Time: %s", resolvedTime));

        if (alert.getAddress() != null && !alert.getAddress().isEmpty()) {
            holder.textViewLocation.setText(String.format("Location: %s", alert.getAddress()));
        } else {
            holder.textViewLocation.setText(String.format(Locale.getDefault(),
                    "Location: %.6f, %.6f", alert.getLatitude(), alert.getLongitude()));
        }

        if (alert.getUserPhone() != null && !alert.getUserPhone().isEmpty()) {
            holder.textViewPhone.setText(String.format("Phone: %s", alert.getUserPhone()));
            holder.textViewPhone.setVisibility(View.VISIBLE);
        } else {
            holder.textViewPhone.setVisibility(View.GONE);
        }

        holder.textViewCategory.setText(alert.getCategory() != null ? alert.getCategory() : "Panic Alert");
        holder.textViewCategory.setTextColor(Color.RED);

        if (alert.getEmergencyContactsList() != null && !alert.getEmergencyContactsList().isEmpty()) {
            StringBuilder contactsText = new StringBuilder("Emergency Contacts:\n");
            for (Map<String, String> contact : alert.getEmergencyContactsList()) {
                contactsText.append("• ")
                        .append(contact.get("name"))
                        .append(" (")
                        .append(contact.get("relationship") != null ? contact.get("relationship") : "No relationship")
                        .append("): ")
                        .append(contact.get("phone"))
                        .append("\n");
            }
            holder.textViewContacts.setText(contactsText.toString());
            holder.textViewContacts.setVisibility(View.VISIBLE);
        } else {
            holder.textViewContacts.setVisibility(View.GONE);
        }

        if (alert.getResolvedBy() != null && !alert.getResolvedBy().isEmpty()) {
            holder.textViewResolvedBy.setText("Resolved by: " + alert.getResolvedBy());
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
    public AlertHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resolved_alert, parent, false);
        return new AlertHolder(view);
    }

    @Override
    public int getItemCount() {
        return filteredAlerts.size();
    }

    static class AlertHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewTime;
        TextView textViewResolvedTime;
        TextView textViewLocation;
        TextView textViewContacts;
        TextView textViewCategory;
        TextView textViewPhone;
        TextView textViewResolvedBy;

        AlertHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.alertUserName);
            textViewTime = itemView.findViewById(R.id.alertTime);
            textViewResolvedTime = itemView.findViewById(R.id.alertResolvedTime);
            textViewLocation = itemView.findViewById(R.id.alertLocation);
            textViewContacts = itemView.findViewById(R.id.alertContacts);
            textViewCategory = itemView.findViewById(R.id.alertCategory);
            textViewPhone = itemView.findViewById(R.id.alertPhone);
            textViewResolvedBy = itemView.findViewById(R.id.alertResolvedBy);
        }
    }
}
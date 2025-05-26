package com.example.pbdv_project;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;

public class PanicAlertAdapter extends FirestoreRecyclerAdapter<PanicAlert, PanicAlertAdapter.AlertHolder> {
    private static final String TAG = "PanicAlertAdapter";
    private OnAlertClickListener listener;
    private boolean showResolveButton;
    private List<PanicAlert> localAlerts = new ArrayList<>();
    private String selectedAlertId = null;

    public interface OnAlertClickListener {
        void onAlertClick(PanicAlert alert);
        void onResolveClick(PanicAlert alert, int position);
        void onDataLoaded(boolean hasData);
    }

    public PanicAlertAdapter(@NonNull FirestoreRecyclerOptions<PanicAlert> options,
                             OnAlertClickListener listener,
                             boolean showResolveButton) {
        super(options);
        this.listener = listener;
        this.showResolveButton = showResolveButton;
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();

        new Handler(Looper.getMainLooper()).post(() -> {
            syncLocalAlerts();
            Log.d(TAG, "Data changed. Total alerts: " + localAlerts.size());
            if (listener != null) {
                listener.onDataLoaded(localAlerts.size() > 0);
            }
            notifyDataSetChanged();
        });
    }

    private void syncLocalAlerts() {
        localAlerts.clear();
        for (int i = 0; i < super.getItemCount(); i++) {
            PanicAlert alert = super.getItem(i);
            if (alert != null && "active".equals(alert.getStatus())) {
                localAlerts.add(alert);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_panic_alert, parent, false);
        return new AlertHolder(v);
    }

    public void setSelectedAlertId(String alertId) {
        this.selectedAlertId = alertId;
        notifyDataSetChanged();
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindViewHolder(@NonNull AlertHolder holder, int position, @NonNull PanicAlert model) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date(model.getTimestamp()));

        holder.textViewName.setText(model.getUserName() != null ?
                model.getUserName() : "Unknown User");
        holder.textViewTime.setText(time);

        String phoneText = model.getUserPhone() != null ?
                "\nPhone: " + model.getUserPhone() : "";
        holder.textViewLocation.setText(String.format(Locale.getDefault(),
                "Location: %.6f, %.6f%s",
                model.getLatitude(),
                model.getLongitude(),
                phoneText));

        if (model.getEmergencyContactsList() != null && !model.getEmergencyContactsList().isEmpty()) {
            StringBuilder contactsText = new StringBuilder("Emergency Contacts:\n");
            for (Map<String, String> contact : model.getEmergencyContactsList()) {
                if (contact != null) {
                    contactsText.append("• ")
                            .append(contact.get("name") != null ? contact.get("name") : "Unknown")
                            .append(" (")
                            .append(contact.get("relationship") != null ?
                                    contact.get("relationship") : "No relationship")
                            .append("): ")
                            .append(contact.get("phone") != null ?
                                    contact.get("phone") : "No phone number")
                            .append("\n");
                }
            }
            holder.textViewContacts.setText(contactsText.toString());
            holder.textViewContacts.setVisibility(View.VISIBLE);
        } else {
            holder.textViewContacts.setVisibility(View.GONE);
        }

        holder.resolveButton.setVisibility(showResolveButton ? View.VISIBLE : View.GONE);
        holder.resolveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResolveClick(model, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return localAlerts.size();
    }

    @Override
    public PanicAlert getItem(int position) {
        return localAlerts.get(position);
    }

    public void removeAlert(int position) {
        if (position >= 0 && position < localAlerts.size()) {
            localAlerts.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, localAlerts.size());
            Log.d(TAG, "Removed alert at position: " + position);
        }
    }

    class AlertHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewTime;
        TextView textViewLocation;
        TextView textViewContacts;
        Button resolveButton;

        public AlertHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.alertUserName);
            textViewTime = itemView.findViewById(R.id.alertTime);
            textViewLocation = itemView.findViewById(R.id.alertLocation);
            textViewContacts = itemView.findViewById(R.id.alertContacts);
            resolveButton = itemView.findViewById(R.id.resolveButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    PanicAlert alert = getItem(position);
                    if (alert != null) {
                        listener.onAlertClick(alert);
                    }
                }
            });
        }
    }
}
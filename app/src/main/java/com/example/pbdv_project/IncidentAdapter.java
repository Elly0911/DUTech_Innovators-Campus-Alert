package com.example.pbdv_project;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncidentAdapter extends FirestoreRecyclerAdapter<Incident, IncidentAdapter.IncidentHolder> {
    private static final String TAG = "IncidentAdapter";
    private OnIncidentClickListener listener;
    private Context context;
    private String targetIncidentId = null;
    private List<Incident> localIncidents = new ArrayList<>();

    public interface OnIncidentClickListener {
        void onIncidentClick(Incident incident);
        void onResolveClick(Incident incident, int position);
        void onDataLoaded(boolean hasData);
    }

    public IncidentAdapter(@NonNull FirestoreRecyclerOptions<Incident> options,
                           OnIncidentClickListener listener) {
        super(options);
        this.listener = listener;
    }

    public void setTargetIncidentId(String incidentId) {
        this.targetIncidentId = incidentId;
        notifyDataSetChanged();
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();

        new Handler(Looper.getMainLooper()).post(() -> {
            syncLocalIncidents();
            Log.d(TAG, "Data changed. Total incidents: " + localIncidents.size());
            if (listener != null) {
                listener.onDataLoaded(localIncidents.size() > 0);
            }
            notifyDataSetChanged();
        });
    }

    private void syncLocalIncidents() {
        localIncidents.clear();
        for (int i = 0; i < super.getItemCount(); i++) {
            Incident incident = super.getItem(i);
            if (incident != null && "pending".equals(incident.getStatus())) {
                localIncidents.add(incident);
            }
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull IncidentHolder holder, int position, @NonNull Incident model) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date(model.getTimestamp()));

        holder.textViewType.setText(model.getType());
        holder.textViewTime.setText(time);
        holder.textViewDescription.setText(model.getDescription());
        holder.textViewLocation.setText("Location: " + model.getLocationText());

        if (model.isAnonymous()) {
            holder.textViewReporter.setText("Anonymous Report");
            holder.textViewContact.setVisibility(View.GONE);
        } else {
            holder.textViewReporter.setText(model.getUserName() != null && !model.getUserName().isEmpty() ?
                    "Reported by: " + model.getUserName() : "Unknown Reporter");

            StringBuilder contactInfo = new StringBuilder();
            if (model.getUserEmail() != null && !model.getUserEmail().isEmpty()) {
                contactInfo.append("Email: ").append(model.getUserEmail());
            }
            if (model.getUserPhone() != null && !model.getUserPhone().isEmpty()) {
                if (contactInfo.length() > 0) contactInfo.append("\n");
                contactInfo.append("Phone: ").append(model.getUserPhone());
            }

            if (contactInfo.length() > 0) {
                holder.textViewContact.setText(contactInfo.toString());
                holder.textViewContact.setVisibility(View.VISIBLE);
            } else {
                holder.textViewContact.setVisibility(View.GONE);
            }
        }

        if (model.isHasMedia() && model.getMediaUrl() != null) {
            holder.mediaCardView.setVisibility(View.VISIBLE);

            if ("image".equals(model.getMediaType())) {
                holder.tvMediaType.setText("Photo Evidence");
                holder.ivMediaPreview.setVisibility(View.VISIBLE);
                holder.vvMediaPreview.setVisibility(View.GONE);

                Glide.with(holder.itemView.getContext())
                        .load(model.getMediaUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_broken_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .override(800, 600))
                        .into(holder.ivMediaPreview);
            } else if ("video".equals(model.getMediaType())) {
                holder.tvMediaType.setText("Video Evidence");
                holder.ivMediaPreview.setVisibility(View.VISIBLE);
                holder.vvMediaPreview.setVisibility(View.GONE);

                Glide.with(holder.itemView.getContext())
                        .load(model.getMediaUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_broken_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .frame(1000)
                                .override(800, 600))
                        .into(holder.ivMediaPreview);
            }
        } else {
            holder.mediaCardView.setVisibility(View.GONE);
        }

        holder.resolveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResolveClick(model, position);
            }
        });

        // Highlight target incident if set
        if (targetIncidentId != null && targetIncidentId.equals(model.getDocumentId())) {
            holder.itemView.setBackgroundResource(R.color.highlight_color);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
    }

    @NonNull
    @Override
    public IncidentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_incident, parent, false);
        return new IncidentHolder(v);
    }

    @Override
    public int getItemCount() {
        return localIncidents.size();
    }

    @Override
    public Incident getItem(int position) {
        return localIncidents.get(position);
    }

    public void removeIncident(int position) {
        if (position >= 0 && position < localIncidents.size()) {
            localIncidents.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, localIncidents.size());
            Log.d(TAG, "Removed incident at position: " + position);
        }
    }

    class IncidentHolder extends RecyclerView.ViewHolder {
        TextView textViewType, textViewTime, textViewDescription, textViewReporter;
        TextView textViewContact, textViewLocation, tvMediaType;
        Button resolveButton;
        MaterialCardView mediaCardView;
        PhotoView ivMediaPreview;
        VideoView vvMediaPreview;

        public IncidentHolder(View itemView) {
            super(itemView);
            textViewType = itemView.findViewById(R.id.incidentType);
            textViewTime = itemView.findViewById(R.id.incidentTime);
            textViewDescription = itemView.findViewById(R.id.incidentDescription);
            textViewReporter = itemView.findViewById(R.id.incidentReporter);
            textViewContact = itemView.findViewById(R.id.incidentContact);
            textViewLocation = itemView.findViewById(R.id.incidentLocation);
            tvMediaType = itemView.findViewById(R.id.tvMediaType);
            resolveButton = itemView.findViewById(R.id.resolveButton);
            mediaCardView = itemView.findViewById(R.id.mediaCardView);
            ivMediaPreview = itemView.findViewById(R.id.ivMediaPreview);
            vvMediaPreview = itemView.findViewById(R.id.vvMediaPreview);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Incident incident = getItem(position);
                    if (incident != null) {
                        listener.onIncidentClick(incident);
                    }
                }
            });
        }
    }
}
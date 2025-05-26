package com.example.pbdv_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class EmergencyContactAdapter extends FirestoreRecyclerAdapter<EmergencyContact, EmergencyContactAdapter.ContactHolder> {
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public EmergencyContactAdapter(@NonNull FirestoreRecyclerOptions<EmergencyContact> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ContactHolder holder, int position, @NonNull EmergencyContact model) {
        holder.textViewName.setText(model.getName());
        holder.textViewPhone.setText(model.getPhone());
        holder.textViewRelationship.setText(model.getRelationship() != null ? model.getRelationship() : "");
    }

    @NonNull
    @Override
    public ContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactHolder(view);
    }

    class ContactHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewPhone;
        TextView textViewRelationship;

        public ContactHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewPhone = itemView.findViewById(R.id.text_view_phone);
            textViewRelationship = itemView.findViewById(R.id.text_view_relationship);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getSnapshots().getSnapshot(position), position);
                }
            });
        }
    }
}
package com.example.pbdv_project;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.util.ArrayList;

public class SafetyTipsAdapter extends RecyclerView.Adapter<SafetyTipsAdapter.SafetyTipViewHolder> {

    private List<SafetyTip> safetyTips;
    private List<Boolean> expandedStates;

    public SafetyTipsAdapter(List<SafetyTip> safetyTips) {
        this.safetyTips = safetyTips;
        this.expandedStates = new ArrayList<>();
        for (int i = 0; i < safetyTips.size(); i++) {
            expandedStates.add(false);
        }
    }

    @NonNull
    @Override
    public SafetyTipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_safety_tip, parent, false);
        return new SafetyTipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SafetyTipViewHolder holder, int position) {
        SafetyTip tip = safetyTips.get(position);

        String title = tip.getTitle();
        if (title.contains(" ")) {
            String[] parts = title.split(" ", 2);
            if (parts[0].length() <= 2) {
                holder.emojiTextView.setText(parts[0]);
                holder.emojiTextView.setVisibility(View.VISIBLE);
                holder.titleTextView.setText(parts[1]);
            } else {
                holder.emojiTextView.setVisibility(View.GONE);
                holder.titleTextView.setText(title);
            }
        } else {
            holder.emojiTextView.setVisibility(View.GONE);
            holder.titleTextView.setText(title);
        }

        holder.instructionsTextView.setText(tip.getInstructions());

        // Set the expanded state
        boolean isExpanded = expandedStates.get(position);
        holder.contentLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandCollapseIcon.setImageResource(isExpanded ?
                R.drawable.ic_expand_less : R.drawable.ic_expand_more);

        // Apply color coding when expanded
        if (isExpanded) {
            int color = ContextCompat.getColor(holder.itemView.getContext(), tip.getColorResId());
            holder.contentLayout.setBackgroundColor(adjustAlpha(color, 0.15f));
            holder.cardView.setStrokeColor(ColorStateList.valueOf(color));
            holder.cardView.setStrokeWidth(3);
        } else {
            holder.cardView.setStrokeWidth(0);
            holder.contentLayout.setBackgroundColor(Color.WHITE);
        }

        holder.headerLayout.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                boolean expanded = !expandedStates.get(adapterPosition);
                expandedStates.set(adapterPosition, expanded);

                if (expanded) {
                    holder.contentLayout.setVisibility(View.VISIBLE);
                    animateExpand(holder);

                    // Apply color coding
                    int color = ContextCompat.getColor(v.getContext(), tip.getColorResId());
                    holder.contentLayout.setBackgroundColor(adjustAlpha(color, 0.15f));
                    holder.cardView.setStrokeColor(ColorStateList.valueOf(color));
                    holder.cardView.setStrokeWidth(3);
                } else {
                    animateCollapse(holder);
                    holder.cardView.setStrokeWidth(0);
                }

                holder.expandCollapseIcon.setImageResource(expanded ?
                        R.drawable.ic_expand_less : R.drawable.ic_expand_more);
            }
        });
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void animateExpand(SafetyTipViewHolder holder) {
        holder.contentLayout.setAlpha(0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(holder.contentLayout, "alpha", 0f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(holder.expandCollapseIcon, "rotation", 0f, 180f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeIn, rotation);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void animateCollapse(SafetyTipViewHolder holder) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(holder.contentLayout, "alpha", 1f, 0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(holder.expandCollapseIcon, "rotation", 180f, 0f);

        fadeOut.setDuration(200);
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                holder.contentLayout.setVisibility(View.GONE);
                holder.contentLayout.setBackgroundColor(Color.WHITE);
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeOut, rotation);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    @Override
    public int getItemCount() {
        return safetyTips.size();
    }

    static class SafetyTipViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView emojiTextView;
        TextView instructionsTextView;
        LinearLayout headerLayout;
        LinearLayout contentLayout;
        ImageView expandCollapseIcon;
        MaterialCardView cardView;

        public SafetyTipViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tip_title);
            emojiTextView = itemView.findViewById(R.id.tip_emoji);
            instructionsTextView = itemView.findViewById(R.id.tip_instructions);
            headerLayout = itemView.findViewById(R.id.header_layout);
            contentLayout = itemView.findViewById(R.id.content_layout);
            expandCollapseIcon = itemView.findViewById(R.id.expand_collapse_icon);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }
}
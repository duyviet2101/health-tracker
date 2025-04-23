package com.example.healthtracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthtracker.R;
import com.example.healthtracker.models.GuideItem;

import java.util.List;

public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {

    private List<GuideItem> guideItems;

    public GuideAdapter(List<GuideItem> guideItems) {
        this.guideItems = guideItems;
    }

    @NonNull
    @Override
    public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide, parent, false);
        return new GuideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
        GuideItem item = guideItems.get(position);
        holder.icon.setImageResource(item.getIconResource());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
    }

    @Override
    public int getItemCount() {
        return guideItems.size();
    }

    static class GuideViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;

        GuideViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.guideIcon);
            title = itemView.findViewById(R.id.guideTitle);
            description = itemView.findViewById(R.id.guideDescription);
        }
    }
}
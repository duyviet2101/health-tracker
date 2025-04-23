package com.example.healthtracker.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.GuideAdapter;
import com.example.healthtracker.models.GuideItem;

import java.util.ArrayList;
import java.util.List;

public class AppGuideActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private GuideAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_guide);

        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerViewGuide);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create guide items
        List<GuideItem> guideItems = createGuideItems();

        // Set up adapter
        adapter = new GuideAdapter(guideItems);
        recyclerView.setAdapter(adapter);
    }

    private List<GuideItem> createGuideItems() {
        List<GuideItem> items = new ArrayList<>();
        
        // Add guide items
        items.add(new GuideItem(
                R.drawable.ic_steps,
                getString(R.string.steps_guide_title),
                getString(R.string.steps_guide_desc)
        ));
        
        items.add(new GuideItem(
                R.drawable.ic_water,
                getString(R.string.water_guide_title),
                getString(R.string.water_guide_desc)
        ));
        
        items.add(new GuideItem(
                R.drawable.ic_stats,
                getString(R.string.stats_guide_title),
                getString(R.string.stats_guide_desc)
        ));
        
        items.add(new GuideItem(
                R.drawable.ic_profile,
                getString(R.string.profile_guide_title),
                getString(R.string.profile_guide_desc)
        ));
        
        items.add(new GuideItem(
                R.drawable.ic_share,
                getString(R.string.share_guide_title),
                getString(R.string.share_guide_desc)
        ));
        
        return items;
    }
}
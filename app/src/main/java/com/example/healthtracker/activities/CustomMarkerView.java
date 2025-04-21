package com.example.healthtracker.activities;

import android.content.Context;
import android.widget.TextView;

import com.example.healthtracker.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class CustomMarkerView extends MarkerView {
    private TextView tvTime;
    private TextView tvValue;
    private String chartType;

    public CustomMarkerView(Context context, int layoutResource, String chartType) {
        super(context, layoutResource);
        this.chartType = chartType != null ? chartType : "steps";
        tvTime = findViewById(R.id.tvTime);
        tvValue = findViewById(R.id.tvValue);
    }

    @Override
    public void refreshContent(Entry entry, Highlight highlight) {
        if (entry != null) {
            int hour = (int) entry.getX();
            tvTime.setText(String.format("%02d:00 - Bây giờ", hour));
            
            String value;
            switch (chartType) {
                case "duration":
                    value = String.format("%.0f phút", entry.getY());
                    break;
                case "calories":
                    value = String.format("%.0f kcal", entry.getY());
                    break;
                default:
                    value = String.format("%.0f bước", entry.getY());
                    break;
            }
            tvValue.setText(value);
        }
        super.refreshContent(entry, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10);
    }
} 
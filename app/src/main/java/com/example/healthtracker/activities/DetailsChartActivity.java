package com.example.healthtracker.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.CustomMarkerView;
import com.example.healthtracker.models.ActivityData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailsChartActivity extends AppCompatActivity {
    private BarChart stepsChart;
    private BarChart durationChart;
    private BarChart caloriesChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_chart);

        // Initialize charts
        stepsChart = findViewById(R.id.stepsChart);
        durationChart = findViewById(R.id.durationChart);
        caloriesChart = findViewById(R.id.caloriesChart);

        // Set up back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Load and process data
        ActivityData activityData = loadActivityData();
        if (activityData != null && activityData.getStepsData() != null) {
            setupCharts(activityData);
        }
    }

    private ActivityData loadActivityData() {
        try {
            InputStream is = getResources().openRawResource(R.raw.activity_data);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            is.close();

            Gson gson = new Gson();
            return gson.fromJson(jsonString.toString(), ActivityData.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupCharts(ActivityData activityData) {
        // Process data by hour
        Map<Integer, Integer> hourlySteps = new HashMap<>();
        Map<Integer, Integer> hourlyDuration = new HashMap<>();
        Map<Integer, Integer> hourlyCalories = new HashMap<>();

        // Initialize maps with 0 values for all hours
        for (int i = 0; i < 24; i++) {
            hourlySteps.put(i, 0);
            hourlyDuration.put(i, 0);
            hourlyCalories.put(i, 0);
        }

        // Aggregate data by hour
        for (ActivityData.DayData day : activityData.getStepsData()) {
            for (ActivityData.Activity activity : day.getActivities()) {
                int hour = activity.getHour();
                hourlySteps.put(hour, hourlySteps.get(hour) + activity.getSteps());
                hourlyDuration.put(hour, hourlyDuration.get(hour) + activity.getDurationMinutes());
                hourlyCalories.put(hour, hourlyCalories.get(hour) + activity.getCalories());
            }
        }

        // Sử dụng màu pastel nhạt hơn nữa
        setupBarChart(stepsChart, createBarEntries(hourlySteps), "Số bước", Color.parseColor("#B5EAB5"));  // Lighter pastel green
        setupBarChart(durationChart, createBarEntries(hourlyDuration), "Thời gian hoạt động", Color.parseColor("#B5D9EA"));  // Lighter pastel blue
        setupBarChart(caloriesChart, createBarEntries(hourlyCalories), "Calo từ hoạt động", Color.parseColor("#EAB5C6"));  // Lighter pastel pink
    }

    private List<BarEntry> createBarEntries(Map<Integer, Integer> hourlyData) {
        List<BarEntry> entries = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            entries.add(new BarEntry(hour, hourlyData.get(hour)));
        }
        return entries;
    }

    private void setupBarChart(BarChart chart, List<BarEntry> entries, String label, int color) {
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(color);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        chart.setData(barData);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setExtraTopOffset(24f);
        chart.setExtraBottomOffset(10f);

        // Customize X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setAxisLineWidth(1f);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(24f);  // Extended to show (giờ)
        xAxis.setLabelCount(6, true);  // 5 labels + (giờ)
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setTextSize(11f);
        xAxis.setYOffset(5f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final String[] labels = new String[]{"0", "6", "12", "18", "(giờ)"};
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "0";
                if (value == 6) return "6";
                if (value == 12) return "12";
                if (value == 18) return "18";
                if (value == 24) return "(giờ)";
                return "";
            }
        });

        // Disable Y axis
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        // Set chart padding
        chart.setViewPortOffsets(30f, 20f, 30f, 30f);

        // Enable touch gestures
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);

        // Add animation
        chart.animateY(1000);

        // Add marker view
        String chartType = "steps";
        if (label.contains("Thời gian")) {
            chartType = "duration";
        } else if (label.contains("Calo")) {
            chartType = "calories";
        }
        
        CustomMarkerView markerView = new CustomMarkerView(this, R.layout.marker_view, chartType);
        markerView.setChartView(chart);
        chart.setMarker(markerView);
        
        chart.invalidate();
    }
}
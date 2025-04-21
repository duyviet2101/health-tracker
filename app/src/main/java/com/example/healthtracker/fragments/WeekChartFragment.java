// WeekChartFragment.java
package com.example.healthtracker.fragments;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.healthtracker.R;
import com.example.healthtracker.models.WeekStepData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WeekChartFragment extends Fragment {

    private static final String ARG_WEEK_DATA = "week_data";
    private WeekStepData weekData;

    public static WeekChartFragment newInstance(WeekStepData weekData) {
        WeekChartFragment fragment = new WeekChartFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WEEK_DATA, weekData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            weekData = (WeekStepData) getArguments().getSerializable(ARG_WEEK_DATA);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (weekData == null) return;

        BarChart barChart = view.findViewById(R.id.barChart);
        List<String> days = Arrays.asList("T2", "T3", "T4", "T5", "T6", "T7", "CN");

        List<BarEntry> entries = new ArrayList<>();
        int maxStep = 0;

        for (int i = 0; i < days.size(); i++) {
            String day = days.get(i);
            int steps = 0;
            for (Map.Entry<String, Integer> entry : weekData.stepsPerDay.entrySet()) {
                if (entry.getKey().startsWith(day)) {
                    steps = entry.getValue();
                    break;
                }
            }
            entries.add(new BarEntry(i, steps));
            if (steps > maxStep) maxStep = steps;
        }

        BarDataSet dataSet = new BarDataSet(entries, weekData.weekLabel);
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < days.size() ? days.get(index) : "";
            }
        });
        barChart.setExtraBottomOffset(16f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(500f);
        if (maxStep > 0) {
            float roundedMax = ((maxStep + 499) / 500) * 500;
            leftAxis.setAxisMaximum(roundedMax + 500f);
        }

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setExtraTopOffset(12f);
        barChart.invalidate();
    }
}
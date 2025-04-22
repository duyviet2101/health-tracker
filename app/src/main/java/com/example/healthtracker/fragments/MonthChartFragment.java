package com.example.healthtracker.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthtracker.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MonthChartFragment extends Fragment {

    private static final String ARG_STEPS_MAP = "steps_map";
    private Map<Integer, Integer> stepsMap;
    private OnDaySelectedListener listener;

    public interface OnDaySelectedListener {
        void onDaySelected(int day, int steps);
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.listener = listener;
    }

    public static MonthChartFragment newInstance(Map<Integer, Integer> stepsMap) {
        MonthChartFragment fragment = new MonthChartFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STEPS_MAP, (java.io.Serializable) stepsMap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            stepsMap = (Map<Integer, Integer>) getArguments().getSerializable(ARG_STEPS_MAP);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_month_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (stepsMap == null) return;

        LineChart lineChart = view.findViewById(R.id.lineChart);

        List<Entry> entries = new ArrayList<>();
        int maxSteps = 0;
        int totalDays = 31; // Hiển thị đầy đủ 31 ngày

        for (int i = 1; i <= totalDays; i++) {
            int steps = stepsMap.containsKey(i) ? stepsMap.get(i) : 0;
            entries.add(new Entry(i, steps));
            if (steps > maxSteps) maxSteps = steps;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Bước theo ngày");
        dataSet.setCircleRadius(5f);
        dataSet.setCircleColor(Color.parseColor("#FF4081")); // màu chấm
        dataSet.setColor(Color.parseColor("#3F51B5"));       // màu đường
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircleHole(false); // chấm đặc

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Trục Y trái
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setGranularity(1000f);
        yAxis.setTextColor(Color.BLACK);
        yAxis.setTextSize(12f);

        if (maxSteps > 0) {
            float roundedMax = ((maxSteps + 499) / 500) * 500;
            yAxis.setAxisMaximum(roundedMax + 500f);
        }

        // Trục Y phải
        lineChart.getAxisRight().setEnabled(false);

        // Trục X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setLabelCount(7, true);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int day = (int) value;
                return (day == 1 || day == 6 || day == 11 || day == 16 || day == 21 || day == 26 || day == 31)
                        ? String.valueOf(day) : "";
            }
        });

        // Tối ưu giao diện
        lineChart.setExtraBottomOffset(16f);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setBackgroundColor(Color.WHITE);

        // Xử lý khi bấm vào chấm
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (listener != null) {
                    listener.onDaySelected((int) e.getX(), (int) e.getY());
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });

        lineChart.invalidate();
    }
}

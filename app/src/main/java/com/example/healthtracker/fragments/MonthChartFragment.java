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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MonthChartFragment extends Fragment {

    private static final String ARG_YEAR = "year";
    private static final String ARG_MONTH = "month";
    private static final String ARG_STEPS_MAP = "steps_map";

    private int year;
    private int month;
    private Map<Integer, Integer> stepsMap;
    private OnDaySelectedListener listener;

    public interface OnDaySelectedListener {
        void onDaySelected(int day, int steps);
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.listener = listener;
    }

    public static MonthChartFragment newInstance(int year, int month, Map<Integer, Integer> stepsMap) {
        MonthChartFragment fragment = new MonthChartFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH, month);
        args.putSerializable(ARG_STEPS_MAP, (Serializable) stepsMap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            year = getArguments().getInt(ARG_YEAR);
            month = getArguments().getInt(ARG_MONTH);
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

        // 🔢 Tính số ngày chính xác của tháng đó
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // Lưu ý: Calendar.MONTH bắt đầu từ 0
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int maxSteps = 0;

        for (int i = 1; i <= maxDay; i++) {
            int steps = stepsMap.getOrDefault(i, 0);
            entries.add(new Entry(i, steps));
            if (steps > maxSteps) maxSteps = steps;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Bước theo ngày");
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false); // Chấm tròn đặc
        dataSet.setCircleColor(Color.rgb(255, 111, 0)); // Cam
        dataSet.setColor(Color.rgb(255, 171, 64)); // Đường cam nhạt
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        lineChart.getAxisRight().setEnabled(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setTextColor(Color.BLACK);
        yAxis.setGranularity(1000f);

        if (maxSteps > 0) {
            float roundedMax = ((maxSteps + 499) / 500) * 500;
            yAxis.setAxisMaximum(roundedMax + 500f);
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setLabelRotationAngle(0);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int day = (int) value;
                return (day == 1 || day == 6 || day == 11 || day == 16 || day == 21 || day == 26 || day == 31) ? String.valueOf(day) : "";
            }
        });

        lineChart.setExtraBottomOffset(16f);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (listener != null) {
                    listener.onDaySelected((int) e.getX(), (int) e.getY());
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        lineChart.invalidate();
    }
}

package com.example.healthtracker.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.DailyDetailsActivity;
import com.example.healthtracker.models.WeekStepData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WeekChartFragment extends Fragment {

    private static final String ARG_WEEK_DATA = "week_data";
    private static final String ARG_ORDERED_KEYS = "ordered_keys";

    private WeekStepData weekData;
    private List<String> orderedDayKeys;
    private OnBarSelectedListener listener;
    private OnDailyDetailRequestListener detailListener;

    public interface OnBarSelectedListener {
        void onBarSelected(String date, int steps);
    }
    
    public interface OnDailyDetailRequestListener {
        void onDailyDetailRequested(String date, int steps);
    }

    public void setOnBarSelectedListener(OnBarSelectedListener listener) {
        this.listener = listener;
    }
    
    public void setOnDailyDetailRequestListener(OnDailyDetailRequestListener listener) {
        this.detailListener = listener;
    }

    public static WeekChartFragment newInstance(WeekStepData weekData, List<String> orderedKeys) {
        WeekChartFragment fragment = new WeekChartFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WEEK_DATA, weekData);
        args.putStringArrayList(ARG_ORDERED_KEYS, new ArrayList<>(orderedKeys));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            weekData = (WeekStepData) getArguments().getSerializable(ARG_WEEK_DATA);
            orderedDayKeys = getArguments().getStringArrayList(ARG_ORDERED_KEYS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (weekData == null || orderedDayKeys == null || orderedDayKeys.size() != 7) return;

        BarChart barChart = view.findViewById(R.id.barChart);
        List<BarEntry> entries = new ArrayList<>();
        int maxStep = 0;

        for (int i = 0; i < orderedDayKeys.size(); i++) {
            String fullKey = orderedDayKeys.get(i); // Ví dụ: "T3 2025-04-22"
            int steps = weekData.stepsPerDay.getOrDefault(fullKey, 0);
            entries.add(new BarEntry(i, steps));
            if (steps > maxStep) maxStep = steps;
        }

        BarDataSet dataSet = new BarDataSet(entries, weekData.weekLabel);
        dataSet.setColor(Color.parseColor("#FB8C00"));
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);
        barChart.setFitBars(true);

        // Trục X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < orderedDayKeys.size()) {
                    return orderedDayKeys.get(index).split(" ")[0]; // T2, T3, ...
                }
                return "";
            }
        });

        // Trục Y trái
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(500f);
        if (maxStep > 0) {
            float roundedMax = ((maxStep + 499) / 500) * 500;
            leftAxis.setAxisMaximum(roundedMax + 500f);
        }

        // Trục Y phải
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Tuỳ chỉnh khác
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setExtraBottomOffset(16f);
        barChart.setExtraTopOffset(12f);

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < orderedDayKeys.size()) {
                    String fullKey = orderedDayKeys.get(index);
                    int steps = weekData.stepsPerDay.getOrDefault(fullKey, 0);
                    String[] split = fullKey.split(" ");
                    if (split.length == 2 && listener != null) {
                        listener.onBarSelected(split[1], steps);
                        
                        // Thay vì mở activity mới, gọi callback để hiển thị chi tiết ngay tại màn hình này
                        if (detailListener != null) {
                            detailListener.onDailyDetailRequested(split[1], steps);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        barChart.invalidate();
    }
    
    public List<BarEntry> generateHourlyData(int totalValue) {
        List<BarEntry> entries = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        
        // Phân phối ngẫu nhiên tổng giá trị vào các giờ trong ngày, tập trung chủ yếu vào giờ hoạt động
        float[] hourlyDistribution = new float[24];
        float sum = 0;
        
        // Tạo điểm cao nhất tại 1-2 thời điểm
        int peak1 = 7 + random.nextInt(3); // 7-9h sáng
        int peak2 = 17 + random.nextInt(3); // 17-19h chiều
        
        for (int i = 0; i < 24; i++) {
            if (i < 5) {
                // Rất ít hoạt động 0-5h sáng
                hourlyDistribution[i] = random.nextFloat() * 0.01f;
            } else if (i == peak1 || i == peak2) {
                // Thời điểm cao điểm
                hourlyDistribution[i] = random.nextFloat() * 0.3f + 0.2f;
            } else if ((i > 7 && i < 11) || (i > 16 && i < 20)) {
                // Hoạt động nhiều buổi sáng và chiều
                hourlyDistribution[i] = random.nextFloat() * 0.15f + 0.05f;
            } else if (i >= 23 || i <= 5) {
                // Rất ít hoạt động đêm
                hourlyDistribution[i] = random.nextFloat() * 0.01f;
            } else {
                // Hoạt động bình thường các thời điểm khác
                hourlyDistribution[i] = random.nextFloat() * 0.07f + 0.03f;
            }
            sum += hourlyDistribution[i];
        }
        
        // Chuẩn hóa tổng phân phối thành 1
        for (int i = 0; i < 24; i++) {
            hourlyDistribution[i] /= sum;
            int value = (int)(totalValue * hourlyDistribution[i]);
            entries.add(new BarEntry(i, value));
        }
        
        return entries;
    }
}

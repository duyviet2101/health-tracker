package com.example.healthtracker.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DailyDetailsActivity extends AppCompatActivity {

    private String selectedDate;
    private int stepsCount;
    
    private BarChart stepsChart;
    private BarChart timeChart;
    private BarChart caloriesChart;
    
    private TextView tvDate;
    private TextView tvStepsCount;
    private TextView tvTimeCount;
    private TextView tvCaloriesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_details);
        
        // Lấy dữ liệu từ Intent
        selectedDate = getIntent().getStringExtra("SELECTED_DATE");
        stepsCount = getIntent().getIntExtra("STEPS_COUNT", 0);
        
        // Ánh xạ UI
        initializeViews();
        
        // Hiển thị dữ liệu
        setupData();
        
        // Thiết lập các biểu đồ
        setupStepsChart();
        setupTimeChart();
        setupCaloriesChart();
        
        // Thiết lập nút back
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
    
    private void initializeViews() {
        stepsChart = findViewById(R.id.stepsChart);
        timeChart = findViewById(R.id.timeChart);
        caloriesChart = findViewById(R.id.caloriesChart);
        
        tvDate = findViewById(R.id.tvDate);
        tvStepsCount = findViewById(R.id.tvStepsCount);
        tvTimeCount = findViewById(R.id.tvTimeCount);
        tvCaloriesCount = findViewById(R.id.tvCaloriesCount);
    }
    
    private void setupData() {
        // Format date
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(selectedDate);
            if (date != null) {
                tvDate.setText(outputFormat.format(date));
            } else {
                tvDate.setText(selectedDate);
            }
        } catch (Exception e) {
            tvDate.setText(selectedDate);
        }
        
        // Thiết lập thông tin thống kê
        tvStepsCount.setText(getString(R.string.steps_format, stepsCount));
        
        // Tính toán thời gian hoạt động (giả định 1000 bước ~ 10 phút)
        int timeInMinutes = (stepsCount * 10) / 1000;
        tvTimeCount.setText(getString(R.string.minutes_format, timeInMinutes));
        
        // Tính toán lượng calo (giả định 1000 bước ~ 40 calo)
        int calories = (stepsCount * 40) / 1000;
        tvCaloriesCount.setText(getString(R.string.calories_format, calories));
    }
    
    private void setupStepsChart() {
        setupHourlyChart(stepsChart, generateHourlyData(stepsCount), 
                getResources().getColor(R.color.steps_color, getTheme()));
    }
    
    private void setupTimeChart() {
        int timeInMinutes = (stepsCount * 10) / 1000;
        setupHourlyChart(timeChart, generateHourlyData(timeInMinutes), 
                getResources().getColor(R.color.time_color, getTheme()));
    }
    
    private void setupCaloriesChart() {
        int calories = (stepsCount * 40) / 1000;
        setupHourlyChart(caloriesChart, generateHourlyData(calories), 
                getResources().getColor(R.color.calories_color, getTheme()));
    }
    
    private void setupHourlyChart(BarChart chart, List<BarEntry> entries, int color) {
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(color);
        dataSet.setDrawValues(false);
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        chart.setData(barData);
        
        // Cấu hình trục X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineWidth(1f);
        xAxis.setAxisLineColor(Color.LTGRAY);
        xAxis.setGranularity(6f);
        xAxis.setLabelCount(4, true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hour = (int) value;
                if (hour == 0 || hour == 6 || hour == 12 || hour == 18) {
                    return "";
                }
                return "";
            }
        });
        
        // Ẩn mô tả và trục Y bên phải
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        
        // Ẩn legend
        chart.getLegend().setEnabled(false);
        
        // Ẩn chức năng kéo, thu phóng
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        
        // Cấu hình trục Y bên trái
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getAxisLeft().setDrawAxisLine(false);
        
        // Canh lề cho biểu đồ
        chart.setExtraOffsets(0, 10, 0, 0);
        chart.setViewPortOffsets(0, 10, 0, 10);
        
        // Thiết lập không có viền highlight khi chạm vào biểu đồ
        chart.setHighlightPerTapEnabled(false);
        
        // Refresh biểu đồ
        chart.invalidate();
    }
    
    private List<BarEntry> generateHourlyData(int totalValue) {
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
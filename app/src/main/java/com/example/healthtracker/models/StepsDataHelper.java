package com.example.healthtracker.models;

import android.content.Context;
import android.util.Log;

import com.example.healthtracker.models.StepsDataResponse;
import com.google.gson.Gson;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class StepsDataHelper {
    private final Context context;

    public StepsDataHelper(Context context) {
        this.context = context;
    }

    public Map<String, List<String>> getStepsDataPerWeek() {
        Map<String, List<String>> weekDataMap = new LinkedHashMap<>();
        List<StepsDataResponse.DayData> sortedList = new ArrayList<>();

        try {
            // Đọc dữ liệu JSON từ file
            InputStream inputStream = context.getResources().openRawResource(
                    context.getResources().getIdentifier("activity_data", "raw", context.getPackageName()));
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            StepsDataResponse dataResponse = gson.fromJson(json, StepsDataResponse.class);

            // Sắp xếp các ngày theo thứ tự
            sortedList = dataResponse.getStepsData();
            Collections.sort(sortedList, Comparator.comparing(StepsDataResponse.DayData::getDate));

        } catch (Exception e) {
            Log.e("StepsDataHelper", "Lỗi đọc dữ liệu: " + e.getMessage());
        }

        // Lập lịch cho các tuần
        List<String> currentWeekData = new ArrayList<>();
        int weekIndex = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Set<String> processedDates = new HashSet<>();

        for (StepsDataResponse.DayData dayData : sortedList) {
            try {
                // Phân tích ngày và chuẩn bị tuần mới
                Date currentDate = dateFormat.parse(dayData.getDate());
                calendar.setTime(currentDate);

                // Tìm ngày thứ 2 gần nhất của tuần này
                Calendar weekStart = (Calendar) calendar.clone();
                int currentDayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK);
                int daysFromMonday = (currentDayOfWeek + 5) % 7; // CN=1, T2=2,... => T2 là 0
                weekStart.add(Calendar.DAY_OF_MONTH, -daysFromMonday);

                // Kiểm tra xem tuần này đã được thêm chưa
                String weekKey = "week" + weekIndex;

                // Đảm bảo dữ liệu không bị lặp
                if (!processedDates.contains(dayData.getDate())) {
                    processedDates.add(dayData.getDate());

                    String dayLabel = getDayLabel(calendar.get(Calendar.DAY_OF_WEEK));
                    int steps = dayData.getActivities().stream().mapToInt(a -> a.getSteps()).sum();
                    currentWeekData.add(dayLabel + " " + dayData.getDate() + " " + steps);
                }

                // Nếu đã có đủ 7 ngày trong tuần
                if (currentWeekData.size() == 7) {
                    weekDataMap.put(weekKey, new ArrayList<>(currentWeekData));
                    currentWeekData.clear();
                    weekIndex++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Xử lý tuần cuối cùng nếu còn lại ngày chưa đủ
        if (!currentWeekData.isEmpty()) {
            for (int i = currentWeekData.size(); i < 7; i++) {
                Calendar weekStart = Calendar.getInstance();
                weekStart.add(Calendar.DAY_OF_MONTH, i);
                String dateStr = dateFormat.format(weekStart.getTime());
                String dayLabel = getDayLabel(weekStart.get(Calendar.DAY_OF_WEEK));
                currentWeekData.add(dayLabel + " " + dateStr + " 0");
            }
            weekDataMap.put("week" + weekIndex, new ArrayList<>(currentWeekData));
        }

        return weekDataMap;
    }

    private String getDayLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "T2";
            case Calendar.TUESDAY: return "T3";
            case Calendar.WEDNESDAY: return "T4";
            case Calendar.THURSDAY: return "T5";
            case Calendar.FRIDAY: return "T6";
            case Calendar.SATURDAY: return "T7";
            case Calendar.SUNDAY: return "CN";
            default: return "";
        }
    }
}

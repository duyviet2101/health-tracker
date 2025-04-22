package com.example.healthtracker;


import android.content.Context;
import android.util.Log;

import com.example.healthtracker.models.StepActivityEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;



public class JsonExporter {
    private static final String TAG = "JsonExporter";
    private static final String FILE_NAME = "all_steps_data.json";

    public static void exportDailySteps(Context context, List<StepActivityEntry> activityList) {
        try {
            // Lấy thông tin ngày hôm nay
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(new Date());

            Calendar cal = Calendar.getInstance();
            String dayOfWeek = new SimpleDateFormat("EEE", Locale.getDefault()).format(cal.getTime());

            DailyStepsData todayData = new DailyStepsData(date, dayOfWeek, activityList);

            // File lưu trữ chính
            File file = new File(context.getExternalFilesDir(null), FILE_NAME);
            List<DailyStepsData> allData = new ArrayList<>();

            // Nếu file đã tồn tại, đọc dữ liệu cũ
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                Type type = new TypeToken<Map<String, List<DailyStepsData>>>(){}.getType();
                Map<String, List<DailyStepsData>> existing = new Gson().fromJson(reader, type);
                if (existing != null && existing.containsKey("stepsData")) {
                    allData = existing.get("stepsData");
                }
                reader.close();
            }

            // Kiểm tra xem đã có dữ liệu cho ngày này chưa để thực hiện ghi đè
            boolean updated = false;
            for (int i = 0; i < allData.size(); i++) {
                if (allData.get(i).date.equals(todayData.date)) {
                    allData.set(i, todayData); // Ghi đè
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                allData.add(todayData);
            }

            // Ghi lại toàn bộ vào file
            Map<String, Object> exportMap = new HashMap<>();
            exportMap.put("stepsData", allData);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(exportMap);

            FileWriter writer = new FileWriter(file);
            writer.write(jsonString);
            writer.flush();
            writer.close();

            Log.d(TAG, "Dữ liệu nhiều ngày đã được ghi vào file: " + file.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi xuất JSON toàn bộ:", e);
        }
    }
}

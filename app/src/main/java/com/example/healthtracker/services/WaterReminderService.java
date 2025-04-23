package com.example.healthtracker.services;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.WaterTrackingActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WaterReminderService {
    private static final String TAG = "WaterReminderService";
    private static final String CHANNEL_ID = "water_reminder_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private final Context context;
    private final AlarmManager alarmManager;
    private final FirebaseFirestore db;
    private String userId;
    
    // Dữ liệu cấu hình
    private boolean reminderEnabled = false;
    private List<Integer> selectedDays = new ArrayList<>();
    private int startHour = 8;
    private int startMinute = 0;
    private int endHour = 22;
    private int endMinute = 0;
    private int reminderIntervalMinutes = 120; // Mặc định 2 giờ

    public WaterReminderService(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.db = FirebaseFirestore.getInstance();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            userId = "anonymous";
        }
        
        // Tạo kênh thông báo (từ Android 8.0 trở lên)
        createNotificationChannel();
    }
    
    /**
     * Tạo kênh thông báo (yêu cầu từ Android 8.0 trở lên)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Nhắc nhở uống nước";
            String description = "Thông báo nhắc nhở uống nước";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Lên lịch thông báo dựa trên cài đặt người dùng
     */
    public void scheduleReminders() {
        // Kiểm tra quyền trên Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()) {
            Log.w(TAG, "Không có quyền SCHEDULE_EXACT_ALARM, không thể đặt báo thức chính xác");
            return;
        }

        // Tải cài đặt người dùng trước
        loadReminderSettings(() -> {
            // Sau khi tải xong, kiểm tra xem thông báo có được bật không
            if (!reminderEnabled || selectedDays.isEmpty()) {
                cancelAllReminders();
                return;
            }
            
            // Xóa tất cả thông báo cũ trước
            cancelAllReminders();
            
            // Lên lịch thông báo mới
            scheduleNextReminder();
        });
    }
    
    /**
     * Lên lịch cho thông báo tiếp theo
     */
    private void scheduleNextReminder() {
        Calendar calendar = Calendar.getInstance();
        int currentDay = (calendar.get(Calendar.DAY_OF_WEEK) - 1); // Chuyển đổi để Chủ nhật = 0, Thứ 2 = 1, ...
        
        if (currentDay == 0) currentDay = 0; // Chủ Nhật
        else currentDay = currentDay; // Các ngày khác
        
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        
        // Tính toán thời gian thông báo tiếp theo
        Calendar nextReminderTime = findNextReminderTime(currentDay, currentHour, currentMinute);
        
        if (nextReminderTime != null) {
            // Đặt thông báo
            Intent intent = new Intent(context, WaterReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Kiểm tra quyền trên Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12 trở lên
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextReminderTime.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    // Không có quyền đặt báo thức chính xác, dùng setAndAllowWhileIdle thay thế
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextReminderTime.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.w(TAG, "Không có quyền SCHEDULE_EXACT_ALARM, sử dụng báo thức không chính xác");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-11
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextReminderTime.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                // Android 5 trở xuống
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        nextReminderTime.getTimeInMillis(),
                        pendingIntent
                );
            }
            
            Log.d(TAG, "Đã lên lịch thông báo tiếp theo: " + nextReminderTime.getTime().toString());
        }
    }
    
    /**
     * Tìm thời gian thích hợp cho thông báo tiếp theo
     */
    private Calendar findNextReminderTime(int currentDay, int currentHour, int currentMinute) {
        Calendar calendar = Calendar.getInstance();
        
        // Kiểm tra thời gian hiện tại có nằm trong khoảng thời gian được cấu hình cho ngày hiện tại không
        boolean isWithinTimeRangeToday = selectedDays.contains(currentDay) && 
                isWithinTimeRange(currentHour, currentMinute);
        
        if (isWithinTimeRangeToday) {
            // Nếu thời gian hiện tại < thời gian bắt đầu thì đặt thông báo vào thời gian bắt đầu
            if (currentHour < startHour || (currentHour == startHour && currentMinute < startMinute)) {
                calendar.set(Calendar.HOUR_OF_DAY, startHour);
                calendar.set(Calendar.MINUTE, startMinute);
                calendar.set(Calendar.SECOND, 0);
                return calendar;
            }
            
            // Nếu thời gian hiện tại + khoảng cách <= thời gian kết thúc thì đặt thông báo vào thời gian hiện tại + khoảng cách
            Calendar nextReminder = (Calendar) calendar.clone();
            nextReminder.add(Calendar.MINUTE, reminderIntervalMinutes);
            
            if (nextReminder.get(Calendar.HOUR_OF_DAY) < endHour || 
                    (nextReminder.get(Calendar.HOUR_OF_DAY) == endHour && 
                    nextReminder.get(Calendar.MINUTE) <= endMinute)) {
                return nextReminder;
            }
        }
        
        // Nếu không thỏa mãn các điều kiện trên, tìm ngày tiếp theo trong danh sách ngày được chọn
        int daysToAdd = 1;
        int nextDay = (currentDay + daysToAdd) % 7;
        
        while (daysToAdd <= 7) {
            if (selectedDays.contains(nextDay)) {
                // Đặt thông báo vào thời gian bắt đầu của ngày tiếp theo
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
                calendar.set(Calendar.HOUR_OF_DAY, startHour);
                calendar.set(Calendar.MINUTE, startMinute);
                calendar.set(Calendar.SECOND, 0);
                return calendar;
            }
            
            daysToAdd++;
            nextDay = (currentDay + daysToAdd) % 7;
        }
        
        return null; // Không tìm thấy thời gian phù hợp
    }
    
    /**
     * Kiểm tra xem thời gian hiện tại có nằm trong khoảng cấu hình
     */
    private boolean isWithinTimeRange(int hour, int minute) {
        int currentTimeMinutes = hour * 60 + minute;
        int startTimeMinutes = startHour * 60 + startMinute;
        int endTimeMinutes = endHour * 60 + endMinute;
        
        return currentTimeMinutes >= startTimeMinutes && currentTimeMinutes <= endTimeMinutes;
    }
    
    /**
     * Hủy tất cả các thông báo đã lên lịch
     */
    public void cancelAllReminders() {
        Intent intent = new Intent(context, WaterReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Đã hủy tất cả các thông báo");
    }
    
    /**
     * Tải cài đặt thông báo từ Firestore
     */
    private void loadReminderSettings(Runnable onComplete) {
        if ("anonymous".equals(userId)) {
            // Nếu không đăng nhập, không có thông báo
            onComplete.run();
            return;
        }
        
        db.collection("users").document(userId)
                .collection("settings").document("water")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        
                        if (document.exists()) {
                            // Tải cài đặt thông báo
                            Boolean isEnabled = document.getBoolean("reminder_enabled");
                            reminderEnabled = (isEnabled != null) ? isEnabled : false;
                            
                            List<Long> days = (List<Long>) document.get("reminder_days");
                            if (days != null && !days.isEmpty()) {
                                selectedDays.clear();
                                for (Long day : days) {
                                    selectedDays.add(day.intValue());
                                }
                            }
                            
                            Long startHourValue = document.getLong("reminder_start_hour");
                            Long startMinuteValue = document.getLong("reminder_start_minute");
                            Long endHourValue = document.getLong("reminder_end_hour");
                            Long endMinuteValue = document.getLong("reminder_end_minute");
                            
                            if (startHourValue != null && startMinuteValue != null &&
                                    endHourValue != null && endMinuteValue != null) {
                                startHour = startHourValue.intValue();
                                startMinute = startMinuteValue.intValue();
                                endHour = endHourValue.intValue();
                                endMinute = endMinuteValue.intValue();
                            }
                            
                            // Tải khoảng thời gian - ưu tiên định dạng mới (phút)
                            Long intervalMinutes = document.getLong("reminder_interval_minutes");
                            if (intervalMinutes != null) {
                                reminderIntervalMinutes = intervalMinutes.intValue();
                            } else {
                                // Định dạng cũ - giờ
                                Long intervalHours = document.getLong("reminder_interval");
                                if (intervalHours != null) {
                                    reminderIntervalMinutes = intervalHours.intValue() * 60;
                                }
                            }
                        }
                    }
                    
                    onComplete.run();
                });
    }

    /**
     * Kiểm tra quyền đặt báo thức chính xác
     * @return true nếu có quyền hoặc API dưới mức cần quyền
     */
    public boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Android 11 trở xuống không cần quyền này
    }

    /**
     * BroadcastReceiver để nhận và xử lý thông báo
     */
    public static class WaterReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Hiển thị thông báo
            showNotification(context);
            
            // Lên lịch cho thông báo tiếp theo
            new WaterReminderService(context).scheduleNextReminder();
        }
        
        private void showNotification(Context context) {
            // Tạo intent khi người dùng nhấn vào thông báo
            Intent intent = new Intent(context, WaterTrackingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_IMMUTABLE
            );
            
            // Xây dựng thông báo
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_water)
                    .setContentTitle("Đã đến giờ uống nước!")
                    .setContentText("Hãy uống nước để giữ sức khỏe và hoàn thành mục tiêu hôm nay.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            // Hiển thị thông báo
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            try {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } catch (SecurityException e) {
                // Xử lý trường hợp không có quyền hiển thị thông báo
                Log.e("WaterReminderReceiver", "Không có quyền hiển thị thông báo: " + e.getMessage());
            }
        }
    }
} 
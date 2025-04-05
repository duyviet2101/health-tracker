package com.example.healthtracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.healthtracker.models.StepHistory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class để quản lý cơ sở dữ liệu SQLite
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBHelper";
    
    // Thông tin về database
    private static final String DATABASE_NAME = "health_tracker.db";
    private static final int DATABASE_VERSION = 1;
    
    // Thông tin về bảng lịch sử bước chân
    private static final String TABLE_STEP_HISTORY = "step_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_STEPS = "steps";
    private static final String COLUMN_DISTANCE = "distance";
    private static final String COLUMN_CALORIES = "calories";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_TARGET_DISTANCE = "target_distance";
    private static final String COLUMN_COMPLETION_PERCENTAGE = "completion_percentage";
    
    // Format ngày tháng cho SQLite
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    /**
     * Constructor
     */
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng lịch sử bước chân
        String CREATE_STEP_HISTORY_TABLE = "CREATE TABLE " + TABLE_STEP_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DATE + " TEXT NOT NULL,"
                + COLUMN_TIMESTAMP + " INTEGER NOT NULL,"
                + COLUMN_STEPS + " INTEGER NOT NULL,"
                + COLUMN_DISTANCE + " REAL NOT NULL,"
                + COLUMN_CALORIES + " INTEGER NOT NULL,"
                + COLUMN_DURATION + " INTEGER NOT NULL,"
                + COLUMN_TARGET_DISTANCE + " REAL DEFAULT 0,"
                + COLUMN_COMPLETION_PERCENTAGE + " INTEGER DEFAULT 100"
                + ")";
        db.execSQL(CREATE_STEP_HISTORY_TABLE);
        
        Log.d(TAG, "Đã tạo bảng " + TABLE_STEP_HISTORY);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Trong trường hợp nâng cấp database, xóa bảng cũ và tạo lại
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STEP_HISTORY);
        onCreate(db);
    }
    
    /**
     * Thêm một bản ghi lịch sử bước chân mới
     */
    public void addStepHistory(String date, String time, int steps, float distance, int calories, long duration, float targetDistance, int completionPercentage) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_TIMESTAMP, time);
        values.put(COLUMN_STEPS, steps);
        values.put(COLUMN_DISTANCE, distance);
        values.put(COLUMN_CALORIES, calories);
        values.put(COLUMN_DURATION, duration);
        values.put(COLUMN_TARGET_DISTANCE, targetDistance);
        values.put(COLUMN_COMPLETION_PERCENTAGE, completionPercentage);
        
        db.insert(TABLE_STEP_HISTORY, null, values);
        db.close();
    }
    
    // Overload để tương thích ngược với code cũ
    public void addStepHistory(String date, String time, int steps, float distance, int calories, long duration) {
        addStepHistory(date, time, steps, distance, calories, duration, distance, 100);
    }
    
    public void addStepHistory(StepHistory history) {
        addStepHistory(
            history.getDate(),
            history.getTime(),
            history.getSteps(),
            history.getDistance(),
            history.getCalories(),
            history.getDuration(),
            history.getTargetDistance(),
            history.getCompletionPercentage()
        );
    }
    
    /**
     * Lấy một bản ghi lịch sử bước chân theo ID
     */
    public StepHistory getStepHistory(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_STEP_HISTORY,
                new String[]{COLUMN_ID, COLUMN_DATE, COLUMN_TIMESTAMP, COLUMN_STEPS, COLUMN_DISTANCE, COLUMN_CALORIES, COLUMN_DURATION, 
                            COLUMN_TARGET_DISTANCE, COLUMN_COMPLETION_PERCENTAGE},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, null
        );
        
        StepHistory stepHistory = null;
        
        if (cursor != null && cursor.moveToFirst()) {
            // Kiểm tra xem có các cột mới hay không
            boolean hasTargetDistance = cursor.getColumnIndex(COLUMN_TARGET_DISTANCE) != -1;
            boolean hasCompletionPercentage = cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE) != -1;
            
            float targetDistance = hasTargetDistance ? cursor.getFloat(7) : cursor.getFloat(4); // Mặc định bằng khoảng cách
            int completionPercentage = hasCompletionPercentage ? cursor.getInt(8) : 100; // Mặc định 100%
            
            stepHistory = new StepHistory(
                    cursor.getInt(0),                                 // ID
                    cursor.getString(1),                              // DATE
                    cursor.getString(2),                              // TIME
                    cursor.getInt(3),                                 // STEPS
                    cursor.getFloat(4),                               // DISTANCE
                    cursor.getInt(5),                                 // CALORIES
                    cursor.getLong(6),                                // DURATION
                    targetDistance,                                   // TARGET_DISTANCE
                    completionPercentage                              // COMPLETION_PERCENTAGE
            );
            
            cursor.close();
        }
        
        db.close();
        return stepHistory;
    }
    
    /**
     * Lấy tất cả bản ghi lịch sử bước chân, sắp xếp theo ngày giảm dần (mới nhất lên đầu)
     */
    public List<StepHistory> getAllStepHistory() {
        List<StepHistory> stepHistoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_STEP_HISTORY,
                null,
                null,
                null,
                null,
                null,
                COLUMN_DATE + " DESC, " + COLUMN_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                // Kiểm tra xem có các cột mới hay không
                boolean hasTargetDistance = cursor.getColumnIndex(COLUMN_TARGET_DISTANCE) != -1;
                boolean hasCompletionPercentage = cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE) != -1;
                
                float targetDistance = hasTargetDistance ? cursor.getFloat(cursor.getColumnIndex(COLUMN_TARGET_DISTANCE)) : cursor.getFloat(cursor.getColumnIndex(COLUMN_DISTANCE));
                int completionPercentage = hasCompletionPercentage ? cursor.getInt(cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE)) : 100;
                
                StepHistory stepHistory = new StepHistory(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_STEPS)),
                        cursor.getFloat(cursor.getColumnIndex(COLUMN_DISTANCE)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_CALORIES)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_DURATION)),
                        targetDistance,
                        completionPercentage
                );
                stepHistoryList.add(stepHistory);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return stepHistoryList;
    }
    
    /**
     * Lấy tất cả bản ghi lịch sử bước chân trong ngày hiện tại
     */
    public List<StepHistory> getTodayStepHistory() {
        List<StepHistory> stepHistoryList = new ArrayList<>();
        
        String today = DATE_FORMAT.format(new Date());
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_STEP_HISTORY,
                null, // Lấy tất cả các cột thay vì chỉ một số cột cụ thể
                COLUMN_DATE + "=?",
                new String[]{today},
                null, null, COLUMN_TIMESTAMP + " DESC", null
        );
        
        if (cursor.moveToFirst()) {
            do {
                // Kiểm tra xem có các cột mới hay không
                boolean hasTargetDistance = cursor.getColumnIndex(COLUMN_TARGET_DISTANCE) != -1;
                boolean hasCompletionPercentage = cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE) != -1;
                
                float targetDistance = hasTargetDistance ? 
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_TARGET_DISTANCE)) : 
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_DISTANCE));
                    
                int completionPercentage = hasCompletionPercentage ? 
                    cursor.getInt(cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE)) : 
                    100;
                
                StepHistory stepHistory = new StepHistory(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_STEPS)),
                        cursor.getFloat(cursor.getColumnIndex(COLUMN_DISTANCE)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_CALORIES)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_DURATION)),
                        targetDistance,
                        completionPercentage
                );
                stepHistoryList.add(stepHistory);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return stepHistoryList;
    }
    
    /**
     * Cập nhật một bản ghi lịch sử bước chân
     */
    public int updateStepHistory(StepHistory stepHistory) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, DATE_FORMAT.format(stepHistory.getDate()));
        values.put(COLUMN_TIMESTAMP, stepHistory.getTime());
        values.put(COLUMN_STEPS, stepHistory.getSteps());
        values.put(COLUMN_DISTANCE, stepHistory.getDistance());
        values.put(COLUMN_CALORIES, stepHistory.getCalories());
        values.put(COLUMN_DURATION, stepHistory.getDuration());
        
        // Cập nhật bản ghi
        int rowsAffected = db.update(
                TABLE_STEP_HISTORY,
                values,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(stepHistory.getId())}
        );
        
        db.close();
        return rowsAffected;
    }
    
    /**
     * Xóa một bản ghi lịch sử bước chân
     */
    public void deleteStepHistory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(
                TABLE_STEP_HISTORY,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}
        );
        db.close();
        
        Log.d(TAG, "Đã xóa bản ghi lịch sử với ID: " + id);
    }
    
    /**
     * Xóa tất cả bản ghi lịch sử bước chân
     */
    public void deleteAllStepHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STEP_HISTORY, null, null);
        db.close();
        
        Log.d(TAG, "Đã xóa tất cả bản ghi lịch sử bước chân");
    }
    
    /**
     * Lấy tổng số bản ghi trong bảng
     */
    public int getStepHistoryCount() {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_STEP_HISTORY;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }

    /**
     * Lấy dữ liệu lịch sử bước theo ngày
     */
    public List<StepHistory> getStepHistoryByDate(String date) {
        List<StepHistory> stepHistoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_STEP_HISTORY,
                null,
                COLUMN_DATE + " = ?",
                new String[]{date},
                null,
                null,
                COLUMN_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                // Kiểm tra xem có các cột mới hay không
                boolean hasTargetDistance = cursor.getColumnIndex(COLUMN_TARGET_DISTANCE) != -1;
                boolean hasCompletionPercentage = cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE) != -1;
                
                float targetDistance = hasTargetDistance ? 
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_TARGET_DISTANCE)) : 
                    cursor.getFloat(cursor.getColumnIndex(COLUMN_DISTANCE));
                    
                int completionPercentage = hasCompletionPercentage ? 
                    cursor.getInt(cursor.getColumnIndex(COLUMN_COMPLETION_PERCENTAGE)) : 
                    100;
                
                StepHistory stepHistory = new StepHistory(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_STEPS)),
                        cursor.getFloat(cursor.getColumnIndex(COLUMN_DISTANCE)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_CALORIES)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_DURATION)),
                        targetDistance,
                        completionPercentage
                );
                stepHistoryList.add(stepHistory);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return stepHistoryList;
    }
} 
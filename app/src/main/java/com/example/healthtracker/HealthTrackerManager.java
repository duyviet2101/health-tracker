package com.example.healthtracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;

/**
 * Lớp xử lý theo dõi sức khỏe gồm:
 * - Đếm bước chân qua cảm biến
 * - Tính khoảng cách di chuyển bằng GPS
 * - Tính lượng calories tiêu hao
 */
public class HealthTrackerManager implements SensorEventListener {

    // Interface để truyền dữ liệu từ class này ra bên ngoài (MainActivity)
    public interface HealthDataListener {
        void onStepCountUpdated(int steps);          // Gửi số bước chân
        void onCaloriesUpdated(double calories);     // Gửi lượng calo đã tiêu hao
        void onDistanceUpdated(double distanceMeters); // Gửi khoảng cách đã di chuyển
    }

    // Các biến phục vụ xử lý bước chân
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    private int initialStepCount = -1;  // Lưu giá trị bước chân ban đầu (tránh đếm sai)
    private int totalSteps = 0;

    // Các biến phục vụ tính khoảng cách bằng GPS
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private Location previousLocation; // Lưu vị trí GPS trước đó để tính khoảng cách
    private double totalDistance = 0;  // Tổng quãng đường đã di chuyển

    private final Context context;
    private final HealthDataListener listener;

    // Constructor khởi tạo class
    public HealthTrackerManager(Context context, HealthDataListener listener) {
        this.context = context;
        this.listener = listener;
        setupStepSensor();  // Cài đặt cảm biến đếm bước
        setupLocation();    // Cài đặt định vị GPS
    }

    // Cấu hình cảm biến bước chân
    private void setupStepSensor() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
        }
    }

    // Bắt đầu theo dõi bước chân và định vị
    public void startTracking() {
        if (isSensorPresent) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        startLocationUpdates();
    }

    // Dừng theo dõi khi không cần
    public void stopTracking() {
        if (isSensorPresent) {
            sensorManager.unregisterListener(this);
        }
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Xử lý sự kiện khi cảm biến thay đổi (bước chân thay đổi)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (initialStepCount == -1) {
            // Lưu số bước chân đầu tiên khi khởi động
            initialStepCount = (int) event.values[0];
        }

        // Tính tổng số bước hiện tại bằng cách lấy số mới - số ban đầu
        totalSteps = (int) event.values[0] - initialStepCount;

        // Gửi số bước và calories ra giao diện
        listener.onStepCountUpdated(totalSteps);
        listener.onCaloriesUpdated(totalSteps * 0.04); // Công thức tính calo
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý phần này
    }

    // Cài đặt định vị GPS
    private void setupLocation() {
        locationClient = LocationServices.getFusedLocationProviderClient(context);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location location : result.getLocations()) {
                    if (previousLocation != null) {
                        // Tính khoảng cách từ vị trí trước đến vị trí mới
                        float distance = previousLocation.distanceTo(location);
                        totalDistance += distance; // Cộng dồn khoảng cách

                        // Gửi dữ liệu khoảng cách ra giao diện
                        listener.onDistanceUpdated(totalDistance);
                    }
                    previousLocation = location;
                }
            }
        };
    }

    // Bắt đầu nhận dữ liệu định vị
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5000); // Cập nhật mỗi 5 giây
        request.setFastestInterval(2000); // Cập nhật nhanh nhất 2 giây



        // Kiểm tra quyền trước khi bắt đầu định vị
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
    }
}

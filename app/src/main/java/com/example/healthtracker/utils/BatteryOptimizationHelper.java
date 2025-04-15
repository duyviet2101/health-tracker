package com.example.healthtracker.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

public class BatteryOptimizationHelper {

    public static boolean isManufacturerWithRestrictions() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("meizu") ||
                manufacturer.contains("oneplus");
    }

    public static void requestBatteryOptimizationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String packageName = context.getPackageName();

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog(context);
            }
        }
    }

    private static void showBatteryOptimizationDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enable Background Activity");
        builder.setMessage("To ensure proper app functionality, please disable battery optimization for this app.");
        builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openBatterySettings(context);
            }
        });
        builder.setNegativeButton("Skip", null);
        builder.show();
    }

    private static void openBatterySettings(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = null;

        if (manufacturer.contains("xiaomi")) {
            intent = getXiaomiIntent(context);
        } else if (manufacturer.contains("oppo")) {
            intent = getOppoIntent(context);
        } else if (manufacturer.contains("vivo")) {
            intent = getVivoIntent(context);
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            intent = getHuaweiIntent(context);
        } else {
            // Default for other devices
            intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback to standard battery optimization settings
            try {
                Intent standardIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                context.startActivity(standardIntent);
            } catch (Exception ex) {
                // If all else fails, open app details settings
                Intent appDetailsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                appDetailsIntent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(appDetailsIntent);
            }
        }
    }

    private static Intent getXiaomiIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        return intent;
    }

    private static Intent getOppoIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
        return intent;
    }

    private static Intent getVivoIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
        return intent;
    }

    private static Intent getHuaweiIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"));
        return intent;
    }
} 
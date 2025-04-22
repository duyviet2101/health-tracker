package com.example.healthtracker.models;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScreenshotSharer {

    public static void captureAndShareScreen(Activity activity) {
        try {
            // Chụp màn hình từ root view
            View view = activity.getWindow().getDecorView().getRootView();
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            // Lưu ảnh vào file tạm
            File imageFile = new File(activity.getExternalCacheDir(), "screenshot.png");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            // Tạo URI thông qua FileProvider
            Uri uri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider", // ví dụ: com.example.healthtracker.fileprovider
                    imageFile
            );

            // Tạo Intent chia sẻ
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Khởi chạy Intent
            activity.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh chụp màn hình"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


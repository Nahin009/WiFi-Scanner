package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WifiForegroundService<Notification> extends Service {
    private static final String CHANNEL_ID = "WifiScanForegroundService";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification for the foreground service
        android.app.Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wi-Fi Scanning Service")
                .setContentText("Scanning Wi-Fi networks in the background")
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .build();

        // Specify foreground service type for Android 12+ (API level 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

        // Continue running the service
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used for foreground service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wi-Fi Scanning Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

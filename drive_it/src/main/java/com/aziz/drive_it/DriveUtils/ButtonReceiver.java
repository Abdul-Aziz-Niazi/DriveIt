package com.aziz.drive_it.DriveUtils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.R;

public class ButtonReceiver extends BroadcastReceiver {
    private static final String TAG = ButtonReceiver.class.getSimpleName();
    private static final String DATA_UPLOAD = "UPLOAD";
    NotificationManager manager;
    SharedPreferences preferences;
    private int icon;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null)
            return;
        preferences = context.getSharedPreferences(DIConstants.PREF_KEY, Context.MODE_PRIVATE);
        Log.d(TAG, "onReceive: " + intent.getAction());

        if (intent.getAction().equalsIgnoreCase("drive_it.cancel")) {
            manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(DIConstants.NOTIFICATION_ID);
        } else if (intent.getAction().equalsIgnoreCase("drive_it.retry")) {
            updateNotification(context);
            String sessionUri = preferences.getString(DIConstants.UPLD_POS, null);
            int count = preferences.getInt(DIConstants.FILE_NUM, 0);
            int startChunk = preferences.getInt(DIConstants.CHNK_START, 0);
            DIFile diFile = DIBackupService.getInstance().getDiFileArrayList().get(count);
            if (sessionUri != null) {
                DIResumeableUpload.getInstance().continueUpload(sessionUri, diFile, 0);
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void updateNotification(Context context) {
        NotificationCompat.Builder notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("Backup in progress")
                .setProgress(10, 0, true)
                .setSound(null)
                .setOngoing(true)
                .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon)
                .setContentText("Processing Backup");
        notificationCompat.mActions.clear();

        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(DATA_UPLOAD) != null)
                manager.deleteNotificationChannel(DATA_UPLOAD);

            manager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        manager.notify(DIConstants.NOTIFICATION_ID, notification);
    }
}

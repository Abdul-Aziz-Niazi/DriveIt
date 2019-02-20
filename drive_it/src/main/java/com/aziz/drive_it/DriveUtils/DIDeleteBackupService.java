package com.aziz.drive_it.DriveUtils;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.R;

import java.util.ArrayList;

public class DIDeleteBackupService extends Service {
    private static final String TAG = DIDeleteBackupService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 119;
    private static final String DATA_DELETE = "DATA_DELETE";
    private boolean show;
    private NotificationCompat.Builder notificationCompat;
    private NotificationManager notificationManager;
    private static DIDeleteBackupService INSTANCE;
    private int total;
    private int count;
    private int errors;
    private int icon;

    public static DIDeleteBackupService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DIDeleteBackupService();
        }
        return INSTANCE;
    }

    public void showNotification(boolean show) {
        this.show = show;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void deleteAll(final Context context, final DICallBack<DIFile> diCallBack) {

        DIFileLister.list(new DICallBack<ArrayList<DIFile>>() {
            @Override
            public void success(ArrayList<DIFile> fileArrayList) {
                total = fileArrayList.size();
                count = 0;
                errors = 0;
                if (checkEmpty(context)) {
                    diCallBack.success(null);
                    return;
                }
                for (DIFile file : fileArrayList) {
                    DIFileDeleter.deleteFile(file.getId(), new DICallBack<DIFile>() {
                        @Override
                        public void success(DIFile file) {
                            Log.d(TAG, "success: " + file.getId());
                            count++;
                            if (total == count) {
                                createNotification(context);
                                DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
                                diCallBack.success(file);
                            }
                        }

                        @Override
                        public void failure(String error) {
                            Log.d(TAG, "failure: " + error);
                            count++;
                            errors++;
                            if (total == count) {
                                diCallBack.success(null);
                                createNotification(context);
                            }
                        }
                    });
                }
            }

            @Override
            public void failure(String error) {
                errors++;
                count++;
                Log.d(TAG, "failure: " + error);
                diCallBack.failure(error);
            }
        });
    }

    private boolean checkEmpty(Context context) {
        if (total == 0) {
            createNotification(context);
            return true;
        }
        return false;
    }


    void createNotification(Context context) {
        Log.d(TAG, "createNotification: creating notification ");

        notificationCompat = new NotificationCompat
                .Builder(context, DATA_DELETE)
                .setSound(null)
                .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon);

        notificationCompat.setContentTitle("Backup removed");
        if (errors != 0) {
            notificationCompat.setContentText(total + " total files " + errors + " errors");
        } else {
            notificationCompat.setContentText(total + " total files");
        }

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(new NotificationChannel(DATA_DELETE, DATA_DELETE, NotificationManager.IMPORTANCE_LOW));
        Notification notification = notificationCompat.build();
        if (total != 0 && show)
            notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}

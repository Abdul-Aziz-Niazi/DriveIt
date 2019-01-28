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
    private NotificationCompat.Builder notificationCompat;
    private NotificationManager notificationManager;
    private static DIDeleteBackupService INSTANCE;
    private int total;

    public static DIDeleteBackupService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DIDeleteBackupService();
        }
        return INSTANCE;
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
                for (DIFile file : fileArrayList) {
                    DIFileDeleter.deleteFile(file.getId(), new DICallBack<DIFile>() {
                        @Override
                        public void success(DIFile file) {
                            Log.d(TAG, "success: " + file.getId());
                            total++;
                            createNotification(context);
                            DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
                            diCallBack.success(file);
                        }

                        @Override
                        public void failure(String error) {
                            Log.d(TAG, "failure: " + error);
                            total++;
                            diCallBack.failure(error);
                        }
                    });
                }
            }

            @Override
            public void failure(String error) {
                total++;
                Log.d(TAG, "failure: " + error);
                diCallBack.failure(error);
            }
        });
    }


    void createNotification(Context context) {
        Log.d(TAG, "createNotification: creating notification ");

        notificationCompat = new NotificationCompat
                .Builder(context, DATA_DELETE)
                .setSound(null)
                .setSmallIcon(R.drawable.ic_gdrive);

        notificationCompat.setContentTitle("Backup removed");
        notificationCompat.setContentText(total + " files");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(new NotificationChannel(DATA_DELETE, DATA_DELETE, NotificationManager.IMPORTANCE_LOW));
        Notification notification = notificationCompat.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}

package com.aziz.drive_it.DriveUtils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIBackupDetails;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.R;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

class DIBackupService extends Service {

    private static DIBackupService INSTANCE;
    private static final String TAG = DIBackupService.class.getSimpleName();
    private static final String DATA_BACKUP = "DATA_BACKUP";
    private static final int NOTIFICATION_ID = 908;
    private NotificationCompat.Builder notificationCompat;
    private NotificationManager notificationManager;
    private int total = 0;
    private int count = 0;
    private Context context;

    public static DIBackupService getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DIBackupService();

        return INSTANCE;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startBackup(Context context, ArrayList<File> fileArrayList, DICallBack<DIFile> fileDICallBack) {
        this.context = context;
        createNotification(context, 0);
        backup(fileArrayList, fileDICallBack);
    }

    private void backup(ArrayList<File> fileArrayList, final DICallBack<DIFile> fileDICallBack) {
        total = fileArrayList.size();
        count = 0;
        Log.d(TAG, "backup: total files " + fileArrayList.size());
        for (final File file : fileArrayList) {
            Log.d(TAG, "backup: " + file.getName());
            backupEach(file, new DICallBack<DIFile>() {
                @Override
                public void success(DIFile diFile) {
                    count++;
                    updateNotification(count);
                    Log.d(TAG, "success: progress " + count + " out of " + total + " " + file.getName());
                    fileDICallBack.success(diFile);
                }

                @Override
                public void failure(String error) {
                    count++;
                    updateNotification(count);
                    Log.d(TAG, "failure: progress " + count + " out of " + total + " " + file.getName() + " " + error);
                    fileDICallBack.failure(error);
                }
            });
        }
    }

    private void backupEach(final File file, final DICallBack<DIFile> diCallBack) {
        DIFile diFile = new DIFile();
        diFile.setName("" + file.getName());
        diFile.setParents(Collections.singletonList("appDataFolder"));
        Call<DIFile> call = DINetworkHandler.getInstance().getWebService().post(DIConstants.LIST_FILES, DINetworkHandler.getHeaders(), diFile);
        call.enqueue(new Callback<DIFile>() {
            @Override
            public void onResponse(Call<DIFile> call, Response<DIFile> response) {
                if (response.isSuccessful()) {
                    DIFile responseFile = response.body();
                    Log.d(TAG, "success: backup " + responseFile);
                    DIFileUploader.uploadFile(responseFile, file, diCallBack);
                } else {
                    diCallBack.failure("Failed to upload metadata" + file.getName());
                    try {
                        String errorBody = response.errorBody().string();
                        Log.d(TAG, "onResponse: Error " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<DIFile> call, Throwable t) {
                diCallBack.failure("Failed to upload " + t.getMessage());
            }
        });

    }


    private void createNotification(Context context, int type) {
        Log.d(TAG, "createNotification: creating notification " + type);
        if (type == 0) {
            notificationCompat = new NotificationCompat
                    .Builder(context, DATA_BACKUP)
                    .setContentTitle("Backup in Progress")
                    .setProgress(10, 0, true)
                    .setSound(null)
                    .setSmallIcon(R.drawable.ic_gdrive)
                    .setContentText("initializing backup");
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (notificationManager.getNotificationChannel(DATA_BACKUP) != null)
                    notificationManager.deleteNotificationChannel(DATA_BACKUP);

                notificationManager.createNotificationChannel(new NotificationChannel(DATA_BACKUP, DATA_BACKUP, NotificationManager.IMPORTANCE_LOW));
            }
            Notification notification = notificationCompat.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            notificationCompat = new NotificationCompat
                    .Builder(context, DATA_BACKUP)
                    .setSound(null)
                    .setSmallIcon(R.drawable.ic_gdrive);
            if (count == 0) {
                notificationCompat.setContentTitle("Backup Failed");
                notificationCompat.setContentText("Files not found");
            } else {
                notificationCompat.setContentTitle("Backup Complete");
                notificationCompat.setContentText(total + " files");
                DIBackupDetailsRepository.getINSTANCE().getBackupDetails(new DICallBack<DIBackupDetails>() {
                    @Override
                    public void success(DIBackupDetails details) {
                        EventBus.getDefault().post(details);
                    }

                    @Override
                    public void failure(String error) {
                        DIBackupDetails errorDetails = new DIBackupDetails();
                        errorDetails.setError(error);
                        EventBus.getDefault().post(errorDetails);
                    }
                });
            }
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                notificationManager.createNotificationChannel(new NotificationChannel(DATA_BACKUP, DATA_BACKUP, NotificationManager.IMPORTANCE_LOW));
            Notification notification = notificationCompat.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

    }

    private void updateNotification(int count) {
        notificationCompat.setProgress(total, count, false);
        notificationCompat.setContentText(count + "/" + total);
        notificationCompat.setSound(null);
        notificationManager.notify(NOTIFICATION_ID, notificationCompat.build());
        if (count == total) {
            DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
            removeNotification();
            createNotification(context, 1);
            stopSelf();
        }
    }

    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

}

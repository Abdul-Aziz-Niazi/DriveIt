package com.aziz.drive_it.DriveUtils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIBackupDetails;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.R;
import com.google.android.gms.drive.Drive;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

public class DIBackupService extends Service {

    private static DIBackupService INSTANCE;
    private static final String TAG = DIBackupService.class.getSimpleName();
    private static final String DATA_BACKUP = "DATA_BACKUP";
    private static final int NOTIFICATION_ID = 908;
    private NotificationCompat.Builder notificationCompat;
    private NotificationManager notificationManager;
    private int total = 0;
    private int count = 0;
    private int icon;
    private Context context;
    private ArrayList<DIFile> diFileArrayList;

    public static DIBackupService getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DIBackupService();

        return INSTANCE;
    }

    public ArrayList<DIFile> getDiFileArrayList() {
        return diFileArrayList;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        INSTANCE = this;
//        Log.d(TAG, "onStartCommand: Worked " + intent.getExtras().getString(DIConstants.DATA, "NULL"));
        Log.d(TAG, "onStartCommand: Worked ");

        if (intent == null || intent.getExtras() == null || !intent.hasExtra(DIConstants.DATA) || !intent.hasExtra(DIConstants.DATA_FILES)) {
            stopForeground(true);
            return START_NOT_STICKY;
        } else {
            Type type = new TypeToken<ArrayList<DIFile>>() {
            }.getType();
            String DATA = intent.getExtras().getString(DIConstants.DATA, "NULL");
            String[] DATA_FILES = intent.getExtras().getStringArray(DIConstants.DATA_FILES);
            icon = intent.getExtras().getInt(DIConstants.DATA_ICON, 0);
            Gson gson = new Gson();
            diFileArrayList = gson.fromJson(DATA, type);
            for (int i = 0; i < diFileArrayList.size(); i++) {
                diFileArrayList.get(i).setFile(new File(DATA_FILES[i]));
            }
            startBackup(getApplicationContext(), diFileArrayList);
            createNotification(getApplicationContext(), 0);
            return super.onStartCommand(intent, flags, startId);
        }

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startBackup(Context context, ArrayList<DIFile> fileArrayList) {
        this.context = context;
        createNotification(context, 0);

        DIResumeableUpload.getInstance().setResumeData(this, fileArrayList);
    }

    public void setIcon(@DrawableRes int icon) {
        this.icon = icon;
    }

    private void backup(final ArrayList<DIFile> fileArrayList, final DICallBack<DIFile> fileDICallBack) {
        total = fileArrayList.size();
        count = 0;
        Log.d(TAG, "backup: total files " + fileArrayList.size());
        final DIFile dDFile = fileArrayList.get(count);
        Log.d(TAG, "backup: " + dDFile.getName());
        DICallBack<DIFile> diCallBack = new DICallBack<DIFile>() {
            @Override
            public void success(DIFile diFile) {
                count++;
                updateNotification(count);

                Log.d(TAG, "success: progress " + count + " out of " + total + " " + diFile.getName());
                fileDICallBack.success(diFile);
                if (fileArrayList.size() != count) {
                    Log.d(TAG, "success: continue " + count);
                    backupEach(fileArrayList.get(count), this);
                }
            }

            @Override
            public void failure(String error) {
                count++;
                updateNotification(count);
                Log.d(TAG, "failure: progress " + count + " out of " + total + " " + dDFile.getName() + " " + error);
                fileDICallBack.failure(error);
                if (fileArrayList.size() != count) {
                    Log.d(TAG, "success: continue " + count);
                    backupEach(fileArrayList.get(count), this);
                }
            }
        };

        backupEach(dDFile, diCallBack);


    }

    private void backupEach(final DIFile file, final DICallBack<DIFile> diCallBack) {
        file.setParents(Collections.singletonList("appDataFolder"));
        Call<DIFile> call = DINetworkHandler.getInstance().getWebService().post(DIConstants.LIST_FILES, DINetworkHandler.getHeaders(), file);
        call.enqueue(new Callback<DIFile>() {
            @Override
            public void onResponse(Call<DIFile> call, Response<DIFile> response) {
                if (response.isSuccessful()) {
                    handleSuccess(response, file, diCallBack);
                } else {
                    handleFailure(response, diCallBack, file);
                }
            }

            @Override
            public void onFailure(Call<DIFile> call, Throwable t) {
                diCallBack.failure("Failed to upload " + t.getMessage());
            }
        });
    }

    private void handleFailure(Response<DIFile> response, DICallBack<DIFile> diCallBack, DIFile file) {
        diCallBack.failure("Failed to upload metadata" + file.getName());
        try {
            String errorBody = response.errorBody().string();
            Log.d(TAG, "onResponse: Error " + errorBody);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSuccess(Response<DIFile> response, DIFile file, DICallBack<DIFile> diCallBack) {
        DIFile responseFile = response.body();
        Log.d(TAG, "success: backup " + responseFile);
        DIFileUploader.uploadFile(responseFile, file, diCallBack);
    }


    private void createNotification(Context context, int type) {
        Log.d(TAG, "createNotification: creating notification " + type);
        if (type == 0) {
            notificationCompat = new NotificationCompat
                    .Builder(context, DATA_BACKUP)
                    .setContentTitle("Backup")
                    .setProgress(10, 0, true)
                    .setSound(null)
                    .setOngoing(true)
                    .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon)
                    .setContentText("Processing backup");
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (notificationManager.getNotificationChannel(DATA_BACKUP) != null)
                    notificationManager.deleteNotificationChannel(DATA_BACKUP);

                notificationManager.createNotificationChannel(new NotificationChannel(DATA_BACKUP, DATA_BACKUP, NotificationManager.IMPORTANCE_LOW));
            }
            Notification notification = notificationCompat.build();
            startForeground(NOTIFICATION_ID, notification);
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            notificationCompat = new NotificationCompat
                    .Builder(context, DATA_BACKUP)
                    .setSound(null)
                    .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon);
            if (count == 0) {
                notificationCompat.setContentTitle("Backup Failed");
                notificationCompat.setContentText("Files not found");
            } else {
//                onBackupComplete();
            }
            notificationCompat.setOngoing(false);
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                notificationManager.createNotificationChannel(new NotificationChannel(DATA_BACKUP, DATA_BACKUP, NotificationManager.IMPORTANCE_LOW));
            Notification notification = notificationCompat.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

    }

    private void onBackupComplete() {
        notificationCompat.setContentTitle("Backup Complete");
        notificationCompat.setContentText(total + " files");
        DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
        DIBackupDetailsRepository.getINSTANCE().getBackupDetails(context, new DICallBack<DIBackupDetails>() {
            @Override
            public void success(DIBackupDetails details) {
                DriveIt.autoBackupComplete(details);
            }

            @Override
            public void failure(String error) {
                DIBackupDetails errorDetails = new DIBackupDetails();
                errorDetails.setError(error);
                DriveIt.autoBackupFailed(errorDetails);
            }
        });
    }

    private void updateNotification(int count) {
        notificationCompat.setProgress(total, count, false);
        notificationCompat.setContentText(Math.round((float) count * 100) / total + "%");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeNotification();
    }
}

package com.aziz.drive_it.DriveUtils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.DriveUtils.utils.DIUtils;
import com.aziz.drive_it.R;
import com.google.gson.reflect.TypeToken;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class DIRestoreService extends Service {
    private static final String TAG = DIRestoreService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 909;
    private static final String DATA_RESTORE = "RESTORE";

    private static DIRestoreService INSTANCE;
    private NotificationCompat.Builder notificationCompat;
    private int count;
    private int total;
    private int errors;
    private NotificationManager notificationManager;
    private Context context;
    private int icon;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null)
            icon = intent.getExtras().getInt(DIConstants.DATA_ICON, 0);
        startRestore(getApplicationContext());
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotification(Context context, int type) {
        Log.d(TAG, "createNotification: creating notification " + type);
        if (type == 0) {
            notificationCompat = new NotificationCompat
                    .Builder(context, DATA_RESTORE)
                    .setContentTitle("Restore in Progress")
                    .setProgress(10, 0, true)
                    .setSound(null)
                    .setSmallIcon(R.drawable.notificaiton_tello_icon)

                    .setContentText("initializing restore");
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(new NotificationChannel(DATA_RESTORE, DATA_RESTORE, NotificationManager.IMPORTANCE_LOW));
            }
            Notification notification = notificationCompat.build();
            startForeground(NOTIFICATION_ID, notification);
            notificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            notificationCompat = new NotificationCompat
                    .Builder(context, DATA_RESTORE)
                    .setSound(null)
                    .setSmallIcon(R.drawable.notificaiton_tello_icon);
            if (count == 0) {
                notificationCompat.setContentTitle("Backup not found");
                notificationCompat.setContentText("Restore failed");
            } else {
                notificationCompat.setContentTitle("Restore Complete");
                if (errors == 0)
                    notificationCompat.setContentText(total + " files");
                else
                    notificationCompat.setContentText(total + " files " + errors + " errors");

            }
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                notificationManager.createNotificationChannel(new NotificationChannel(DATA_RESTORE, DATA_RESTORE, NotificationManager.IMPORTANCE_LOW));
            Notification notification = notificationCompat.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

    }

    private void updateNotification(int count) {
        notificationCompat.setProgress(total, count, false);
        notificationCompat.setContentText(Math.round((float) count * 100) / (total) + "%");
        notificationCompat.setSound(null);
        notificationCompat.setSmallIcon(R.drawable.notificaiton_tello_icon);
        notificationCompat.setOngoing(true);
        notificationManager.notify(NOTIFICATION_ID, notificationCompat.build());
        if (count == total) {
            removeNotification();
            createNotification(context, 1);
            stopSelf();
        }
    }


    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public static DIRestoreService getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DIRestoreService();
        return INSTANCE;
    }

    public void startRestore(Context context) {
        this.context = context;
        createNotification(context,0);
        restore();
    }


    public void restore() {
        DIFileLister.list(new DICallBack<ArrayList<DIFile>>() {
            @Override
            public void success(ArrayList<DIFile> diFileArrayList) {
                Log.d(TAG, "success: " + diFileArrayList.size());
                ArrayList<DIFile> fileArrayList = filterOutFolders(diFileArrayList);
                total = fileArrayList.size();
                count = 0;
                errors = 0;
                Log.d(TAG, "progress: 0 out of " + total);
                if (total == 0) {
                    createNotification(context, 1);
                    stopForeground(true);
                    return;
                }
                for (final DIFile file : fileArrayList) {
                    restoreEach(file, new DICallBack<File>() {
                        @Override
                        public void success(File data) {
                            count++;
                            file.setFile(data);
                            DriveIt.restoreFileComplete(file);
                            updateNotification(count);
                            Log.d(TAG, "progress: " + count + " out of " + total + " " + data.getName() + " " + file.getKind());

                        }

                        @Override
                        public void failure(String error) {
                            if (error != null && error.contains("authError")) {
                                DriveIt.restoreFileFailed(error);
                                authErrorNotification();
                                return;
                            }
                            count++;
                            errors++;
                            DriveIt.restoreFileFailed(error);
                            updateNotification(count);
                            Log.d(TAG, "progress: " + count + " out of " + total + " " + error + " " + file.getKind());
                        }
                    });
                }

            }

            @Override
            public void failure(String error) {
                count++;
                errors++;
                Log.d(TAG, "failure: " + error);
                if (error != null && error.contains("authError")) {
                    DriveIt.restoreFileFailed(error);

                    authErrorNotification();
                    return;
                }
            }
        });

    }

    private void authErrorNotification() {
        notificationCompat = new NotificationCompat
                .Builder(context, DATA_RESTORE)
                .setContentTitle("Restore failed")
                .setSound(null)
                .setSmallIcon(R.drawable.notificaiton_tello_icon)

                .setContentText("Auth Error");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel(DATA_RESTORE, DATA_RESTORE, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }


    private void restoreEach(final DIFile file, final DICallBack<File> completionListener) {
        DIFileDownloader.downloadFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.driveIt/", file,
                new DICallBack<File>() {
                    @Override
                    public void success(File file) {
                        Log.d(TAG, "success: downloaded " + file.getName());
                        completionListener.success(file);
                    }

                    @Override
                    public void failure(String error) {
                        Log.d(TAG, "failure: " + error);
                        completionListener.failure(file.getName() + " failed");
                    }
                });
    }


    private ArrayList<DIFile> filterOutFolders(ArrayList<DIFile> diFileArrayList) {
        ArrayList<DIFile> data = new ArrayList<>();
        for (DIFile file : diFileArrayList) {
            if (file.getMimeType() != null && !file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")) {
                data.add(file);
            }
        }
        return data;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
